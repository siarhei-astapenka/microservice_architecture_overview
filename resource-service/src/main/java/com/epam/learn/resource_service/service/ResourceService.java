package com.epam.learn.resource_service.service;

import com.epam.learn.resource_service.entity.ResourceEntity;
import com.epam.learn.resource_service.exception.NotFoundException;
import com.epam.learn.resource_service.repository.ResourceRepository;
import com.epam.learn.resource_service.service.storage.S3StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final S3StorageService s3StorageService;
    private final String s3Bucket;

    @Autowired
    public ResourceService(ResourceRepository resourceRepository,
                           S3StorageService s3StorageService,
                           @Value("${s3.bucket:resource-bucket}") String s3Bucket) {
        this.resourceRepository = resourceRepository;
        this.s3StorageService = s3StorageService;
        this.s3Bucket = s3Bucket;
    }

    public Map<String, Long> uploadResource(byte[] file) {
        // upload file bytes to S3 and save location in DB
        String key = s3StorageService.upload(file, null);

        ResourceEntity entity = ResourceEntity.builder()
                .storageBucket(s3Bucket)
                .storageKey(key)
                .build();

        entity = resourceRepository.save(entity);

        Map<String, Long> response = new HashMap<>();
        response.put("id", entity.getId());

        return response;
    }

    public byte[] downloadResource(Long id) {
        ResourceEntity entity = resourceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Resource with ID=%s not found", id)));

        if (entity.getStorageKey() == null || entity.getStorageKey().isBlank()) {
            throw new NotFoundException(String.format("Resource with ID=%s not found", id));
        }

        return s3StorageService.download(entity.getStorageKey());
    }

    public Map<String, List<Long>> deleteResources(String ids) {
        Map<String, List<Long>> response = new HashMap<>();

        List<Long> idsToDelete = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());

        List<Long> existingIds = resourceRepository.findExistingIds(idsToDelete);

        for (Long id : existingIds) {
            resourceRepository.findById(id).ifPresent(entity -> {
                if (entity.getStorageKey() != null && !entity.getStorageKey().isBlank()) {
                    try {
                        s3StorageService.delete(entity.getStorageKey());
                    } catch (Exception e) {
                        // log and continue - deletion best-effort
                        // ...existing code...
                    }
                }
            });
        }

        resourceRepository.deleteAllById(existingIds);

        response.put("ids", existingIds);

        return response;
    }
}
