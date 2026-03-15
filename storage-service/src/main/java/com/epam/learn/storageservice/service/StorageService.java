package com.epam.learn.storageservice.service;

import com.epam.learn.storageservice.entity.StorageEntity;
import com.epam.learn.storageservice.exception.StorageAlreadyExistsException;
import com.epam.learn.storageservice.repository.StorageRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.List;
import java.net.URI;
import java.util.Optional;

@Slf4j
@Service
public class StorageService {

    private static final Logger logger = LoggerFactory.getLogger(StorageService.class);

    private final StorageRepository storageRepository;
    private final S3Client s3Client;

    public StorageService(StorageRepository storageRepository,
                          @Value("${s3.endpoint}") String s3Endpoint,
                          @Value("${s3.region}") String s3Region,
                          @Value("${s3.access-key}") String s3AccessKey,
                          @Value("${s3.secret-key}") String s3SecretKey,
                          @Value("${s3.path-style-access}") boolean s3PathStyle) {
        this.storageRepository = storageRepository;
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(s3Endpoint))
                .region(Region.of(s3Region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(s3AccessKey, s3SecretKey)))
                .forcePathStyle(s3PathStyle)
                .build();
    }

    public StorageEntity createStorage(StorageEntity storageEntity) {
        log.debug("Creating storage: {}", storageEntity.getStorageType());
        try {
            StorageEntity savedStorageEntity = storageRepository.save(storageEntity);
            createBucketIfNotExists(savedStorageEntity.getBucket());
            log.info("Created storage: {} with bucket: {}", savedStorageEntity.getStorageType(), savedStorageEntity.getBucket());
            return savedStorageEntity;
        } catch (DataIntegrityViolationException e) {
            log.warn("Storage already exists: {}", storageEntity.getStorageType());
            throw new StorageAlreadyExistsException("Storage with type '" + storageEntity.getStorageType() + "' already exists");
        }
    }

    public List<StorageEntity> getAllStorages() {
        log.debug("Getting all storages");
        List<StorageEntity> storages = storageRepository.findAll();
        log.info("Retrieved {} storages", storages.size());
        return storages;
    }

    public StorageEntity upsertStorage(StorageEntity storageEntity) {
        log.debug("Upserting storage: {}", storageEntity.getStorageType());
        Optional<StorageEntity> existingStorage = storageRepository.findByStorageType(storageEntity.getStorageType());
        
        if (existingStorage.isPresent()) {
            StorageEntity existing = existingStorage.get();
            existing.setBucket(storageEntity.getBucket());
            existing.setPath(storageEntity.getPath());

            StorageEntity updated = storageRepository.save(existing);
            createBucketIfNotExists(updated.getBucket());
            log.info("Updated storage: {} with bucket: {}", updated.getStorageType(), updated.getBucket());
            return updated;
        } else {
            return createStorage(storageEntity);
        }
    }

    public void deleteStorages(List<Long> ids) {
        log.debug("Deleting storages with ids: {}", ids);
        storageRepository.deleteAllByIdInBatch(ids);
        log.info("Deleted {} storages", ids.size());
    }

    private void createBucketIfNotExists(String bucketName) {
        try {
            // Check if bucket exists
            boolean bucketExists = s3Client.listBuckets().buckets().stream()
                    .anyMatch(b -> b.name().equals(bucketName));
            if (!bucketExists) {
                CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                        .bucket(bucketName)
                        .build();
                s3Client.createBucket(createBucketRequest);
                logger.info("Created bucket: {}", bucketName);
            } else {
                logger.info("Bucket {} already exists", bucketName);
            }
        } catch (S3Exception e) {
            logger.error("Error creating bucket {}: {}", bucketName, e.getMessage());
            throw e;
        }
    }
}