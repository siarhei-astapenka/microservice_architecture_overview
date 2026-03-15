package com.epam.learn.resource_service.service;

import com.epam.learn.resource_service.client.StorageServiceClient;
import com.epam.learn.resource_service.client.SongServiceClient;
import com.epam.learn.resource_service.enumeration.ResourceState;
import com.epam.learn.resource_service.enumeration.StorageType;
import com.epam.learn.resource_service.model.ResourceUploadMessage;
import com.epam.learn.resource_service.model.Storage;
import com.epam.learn.resource_service.entity.ResourceEntity;
import com.epam.learn.resource_service.exception.StorageConnectionException;
import com.epam.learn.resource_service.exception.NotFoundException;
import com.epam.learn.resource_service.messaging.ResourceUploadProducer;
import com.epam.learn.resource_service.repository.ResourceRepository;
import com.epam.learn.resource_service.service.storage.S3StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final S3StorageService s3StorageService;
    private final ResourceUploadProducer resourceUploadProducer;
    private final SongServiceClient songServiceClient;
    private final StorageServiceClient storageServiceClient;

    @Autowired
    public ResourceService(ResourceRepository resourceRepository,
                           S3StorageService s3StorageService,
                           ResourceUploadProducer resourceUploadProducer,
                           SongServiceClient songServiceClient,
                           StorageServiceClient storageServiceClient) {
        this.resourceRepository = resourceRepository;
        this.s3StorageService = s3StorageService;
        this.resourceUploadProducer = resourceUploadProducer;
        this.songServiceClient = songServiceClient;
        this.storageServiceClient = storageServiceClient;
    }

    public Map<String, Long> uploadResource(byte[] file) {
        log.info("Uploading resource, size={} bytes", file.length);

        // Generate storageKey as just UUID + .mp3 (no path)
        String storageKey = s3StorageService.generateStorageKey();

        // S3StorageService handles getting bucket/path from Storage Service based on STAGING state
        s3StorageService.upload(file, storageKey, ResourceState.STAGING);
        log.info("Uploaded to S3 staging with key={}", storageKey);

        // Save resource entity with STAGING state and just the storageKey (no bucket/path)
        ResourceEntity entity = saveResourceEntity(storageKey, ResourceState.STAGING);
        log.info("Saved to DB: id={}, state={}", entity.getId(), entity.getState());

        // Get staging storage info for the message
        Storage stagingStorage = storageServiceClient.getStorageByType(StorageType.STAGING)
                .orElseThrow(() -> new StorageConnectionException("STAGING storage not available"));
        
        try {
            ResourceUploadMessage message = ResourceUploadMessage.builder()
                    .resourceId(entity.getId())
                    .storageBucket(stagingStorage.getBucket())
                    .storageKey(s3StorageService.getFullStorageKey(stagingStorage.getPath(), storageKey))
                    .build();
            resourceUploadProducer.sendResourceUploadMessage(message);
        } catch (Exception e) {
            log.error("Failed to send message for resourceId={}: {}", entity.getId(), e.getMessage());
        }

        return Map.of("id", entity.getId());
    }

    @Transactional
    @Retryable(
            retryFor = {org.springframework.dao.DataAccessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, maxDelay = 1000, multiplier = 2)
    )
    public ResourceEntity saveResourceEntity(String storageKey, ResourceState state) {
        ResourceEntity entity = ResourceEntity.builder()
                .storageKey(storageKey)
                .state(state)
                .build();
        return resourceRepository.save(entity);
    }

    @Recover
    public ResourceEntity recoverSaveResourceEntity(org.springframework.dao.DataAccessException e, 
                                                    String storageKey, ResourceState state) {
        log.error("Failed to save to DB after retries for storageKey={}: {}", storageKey, e.getMessage());
        throw new StorageConnectionException("Database unavailable after retries for storageKey: " + storageKey, e);
    }

    public byte[] downloadResource(Long id) {
        ResourceEntity entity = resourceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Resource not found: " + id));

        // S3StorageService handles getting bucket/path from Storage Service based on state
        return s3StorageService.download(entity.getStorageKey(), entity.getState());
    }

    /**
     * Handle processing completion notification from Resource Processor.
     * Moves the file from STAGING to PERMANENT storage.
     */
    @Transactional
    public void handleProcessingComplete(Long resourceId) {
        log.info("Handling processing complete for resourceId={}", resourceId);

        // Get the resource entity
        ResourceEntity entity = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new NotFoundException("Resource not found: " + resourceId));

        // Verify the resource is in STAGING state
        if (entity.getState() != ResourceState.STAGING) {
            log.warn("Resource {} is not in STAGING state, current state: {}", 
                    resourceId, entity.getState());
            return;
        }

        // Generate new storageKey as UUID + .mp3 (no path)
        String newStorageKey = UUID.randomUUID() + ".mp3";
        
        // S3StorageService handles moving from STAGING to PERMANENT (gets bucket/path from Storage Service)
        s3StorageService.move(entity.getStorageKey(), newStorageKey);
        log.info("Moved file from STAGING to PERMANENT: {} -> {}", entity.getStorageKey(), newStorageKey);

        // Update entity with PERMANENT state and new storageKey
        entity.setState(ResourceState.PERMANENT);
        entity.setStorageKey(newStorageKey);
        resourceRepository.save(entity);

        log.info("Updated resource {} to PERMANENT state", resourceId);
    }

    public Map<String, List<Long>> deleteResources(String ids) {
        List<Long> idsToDelete = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();

        List<Long> existingIds = resourceRepository.findExistingIds(idsToDelete);
        log.info("Existing resource ids to delete: {}", existingIds);

        for (Long id : existingIds) {
            resourceRepository.findById(id).ifPresent(entity -> {
                // S3StorageService handles getting bucket/path from Storage Service based on state
                try {
                    s3StorageService.delete(entity.getStorageKey(), entity.getState());
                } catch (Exception e) {
                    log.error("Failed to delete from S3 for id={}: {}", id, e.getMessage());
                }
            });
        }

        resourceRepository.deleteAllById(existingIds);
        log.info("Deleted {} resource rows from repository", existingIds.size());

        try {
            log.info("Requesting song metadata deletion for ids={}", ids);
            songServiceClient.deleteMetadata(ids);
            log.info("Requested song metadata deletion for ids={}", ids);
        } catch (Exception e) {
            log.error("Failed to delete song metadata for ids={}. Error: {}", ids, e.getMessage());
        }

        return Map.of("ids", existingIds);
    }
}
