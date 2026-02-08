package com.epam.learn.resource_service.service;

import com.epam.learn.resource_service.client.SongServiceClient;
import com.epam.learn.resource_service.dto.ResourceUploadMessage;
import com.epam.learn.resource_service.entity.ResourceEntity;
import com.epam.learn.resource_service.exception.NotFoundException;
import com.epam.learn.resource_service.messaging.ResourceUploadProducer;
import com.epam.learn.resource_service.repository.ResourceRepository;
import com.epam.learn.resource_service.service.storage.S3StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final S3StorageService s3StorageService;
    private final String s3Bucket;
    private final ResourceUploadProducer resourceUploadProducer;
    private final SongServiceClient songServiceClient;

    @Autowired
    public ResourceService(ResourceRepository resourceRepository,
                           S3StorageService s3StorageService,
                           @Value("${s3.bucket:resource-bucket}") String s3Bucket,
                           ResourceUploadProducer resourceUploadProducer,
                           SongServiceClient songServiceClient) {
        this.resourceRepository = resourceRepository;
        this.s3StorageService = s3StorageService;
        this.s3Bucket = s3Bucket;
        this.resourceUploadProducer = resourceUploadProducer;
        this.songServiceClient = songServiceClient;
    }

    public Map<String, Long> uploadResource(byte[] file) {
        log.info("Uploading resource, size={} bytes", file.length);

        String key = s3StorageService.upload(file, null);
        log.info("Uploaded to S3: key={}", key);

        ResourceEntity entity = saveResourceEntity(key);
        log.info("Saved to DB: id={}", entity.getId());

        try {
            ResourceUploadMessage message = ResourceUploadMessage.builder()
                    .resourceId(entity.getId())
                    .storageBucket(s3Bucket)
                    .storageKey(key)
                    .build();
            resourceUploadProducer.sendResourceUploadMessage(message);
        } catch (Exception e) {
            log.error("Failed to send message for resourceId={}: {}", entity.getId(), e.getMessage());
        }

        return Map.of("id", entity.getId());
    }

    @Retryable(
            retryFor = {org.springframework.dao.DataAccessException.class},
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public ResourceEntity saveResourceEntity(String key) {
        ResourceEntity entity = ResourceEntity.builder()
                .storageBucket(s3Bucket)
                .storageKey(key)
                .build();
        return resourceRepository.save(entity);
    }

    @Recover
    public ResourceEntity recoverSaveResourceEntity(org.springframework.dao.DataAccessException e, String key) {
        log.error("Failed to save to DB after retries for key={}: {}", key, e.getMessage());
        throw new RuntimeException("Database is down", e); // Пусть упадет - это фатально
    }

    public byte[] downloadResource(Long id) {
        ResourceEntity entity = resourceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Resource not found: " + id));

        return s3StorageService.download(entity.getStorageKey());
    }

    public Map<String, List<Long>> deleteResources(String ids) {
        List<Long> idsToDelete = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());

        List<Long> existingIds = resourceRepository.findExistingIds(idsToDelete);
        log.info("Existing resource ids to delete: {}", existingIds);

        for (Long id : existingIds) {
            resourceRepository.findById(id).ifPresent(entity -> {
                try {
                    s3StorageService.delete(entity.getStorageKey());
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
