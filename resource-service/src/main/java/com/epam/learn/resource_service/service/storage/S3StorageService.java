package com.epam.learn.resource_service.service.storage;

import com.epam.learn.resource_service.client.StorageServiceClient;
import com.epam.learn.resource_service.enumeration.ResourceState;
import com.epam.learn.resource_service.enumeration.StorageType;
import com.epam.learn.resource_service.exception.FileUploadException;
import com.epam.learn.resource_service.exception.StorageConnectionException;
import com.epam.learn.resource_service.model.Storage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.nio.file.AccessDeniedException;
import java.util.UUID;

@Service
@Slf4j
public class S3StorageService {

    private final S3Client s3Client;
    private final StorageServiceClient storageServiceClient;

    public S3StorageService(S3Client s3Client, 
                           StorageServiceClient storageServiceClient) {
        this.s3Client = s3Client;
        this.storageServiceClient = storageServiceClient;
    }

    /**
     * Upload file based on resource state.
     * Bucket and path are obtained from Storage Service based on the state.
     */
    @Retryable(
            retryFor = {S3Exception.class, SdkClientException.class},
            noRetryFor = {NoSuchBucketException.class, AccessDeniedException.class, NoSuchKeyException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, maxDelay = 10000, multiplier = 2)
    )
    public String upload(byte[] data, String storageKey, ResourceState state) {
        Storage storage = getStorageForState(state);
        String bucket = storage.getBucket();
        String path = storage.getPath();
        String fullKey = getFullStorageKey(path, storageKey);
        
        return upload(data, fullKey, bucket);
    }

    /**
     * Download file based on resource state.
     * Bucket and path are obtained from Storage Service based on the state.
     */
    @Retryable(
            retryFor = {S3Exception.class, SdkClientException.class},
            noRetryFor = {NoSuchKeyException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, maxDelay = 10000, multiplier = 2)
    )
    public byte[] download(String storageKey, ResourceState state) {
        Storage storage = getStorageForState(state);
        String bucket = storage.getBucket();
        String path = storage.getPath();
        String fullKey = getFullStorageKey(path, storageKey);
        
        return download(fullKey, bucket);
    }

    /**
     * Delete file based on resource state.
     * Bucket and path are obtained from Storage Service based on the state.
     */
    @Retryable(
            retryFor = {S3Exception.class, SdkClientException.class},
            noRetryFor = {NoSuchKeyException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, maxDelay = 10000, multiplier = 2)
    )
    public void delete(String storageKey, ResourceState state) {
        Storage storage = getStorageForState(state);
        String bucket = storage.getBucket();
        String path = storage.getPath();
        String fullKey = getFullStorageKey(path, storageKey);
        
        delete(fullKey, bucket);
    }

    /**
     * Move file from STAGING to PERMANENT state.
     * Gets source and destination bucket/path from Storage Service.
     */
    public String move(String sourceStorageKey, String destStorageKey) {
        // Get STAGING storage info
        Storage stagingStorage = storageServiceClient.getStorageByType(StorageType.STAGING)
                .orElseThrow(() -> new StorageConnectionException(
                        "STAGING storage not available"));
        
        // Get PERMANENT storage info
        Storage permanentStorage = storageServiceClient.getStorageByType(StorageType.PERMANENT)
                .orElseThrow(() -> new StorageConnectionException(
                        "PERMANENT storage not available"));

        String stagingBucket = stagingStorage.getBucket();
        String stagingPath = stagingStorage.getPath();
        String stagingKey = getFullStorageKey(stagingPath, sourceStorageKey);

        String permanentBucket = permanentStorage.getBucket();
        String permanentPath = permanentStorage.getPath();
        String permanentKey = getFullStorageKey(permanentPath, destStorageKey);

        // Copy to permanent location
        copy(stagingKey, permanentKey, stagingBucket, permanentBucket);
        
        // Delete from staging
        delete(stagingKey, stagingBucket);
        
        log.info("Moved file from STAGING to PERMANENT: {} -> {}", stagingKey, permanentKey);
        return destStorageKey;
    }

    /**
     * Internal upload method with explicit bucket.
     */
    private String upload(byte[] data, String fullStorageKey, String bucket) {
        if (data == null || data.length == 0) {
            throw new FileUploadException("File data must be provided");
        }

        log.info("Uploading to S3: bucket={}, key={}, size={}b", bucket, fullStorageKey, data.length);

        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fullStorageKey)
                .contentType("audio/mpeg")
                .build();

        s3Client.putObject(putReq, RequestBody.fromBytes(data));
        log.info("Uploaded to S3: key={}", fullStorageKey);
        return fullStorageKey;
    }

    /**
     * Internal download method with explicit bucket.
     */
    public byte[] download(String key, String bucket) {
        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        ResponseBytes<?> responseBytes = s3Client.getObjectAsBytes(getReq);
        return responseBytes.asByteArray();
    }

    /**
     * Internal delete method with explicit bucket.
     */
    public void delete(String key, String bucket) {
        DeleteObjectRequest delReq = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        s3Client.deleteObject(delReq);
        log.info("Deleted from S3: key={}", key);
    }

    public String generateStorageKey() {
        return UUID.randomUUID() + ".mp3";
    }

    public String getFullStorageKey(String path, String storageKey) {
        return path + "/" + storageKey;
    }

    @Recover
    public String recoverUpload(Exception e, byte[] data, String key, String bucket) {
        log.error("Failed to upload to S3 after retries: {}", e.getMessage());
        throw new StorageConnectionException("S3 upload failed", e);
    }

    @Recover
    public byte[] recoverDownload(Exception e, String key, String bucket) {
        log.error("Failed to download from S3 after retries: {}", e.getMessage());
        throw new StorageConnectionException("S3 download failed", e);
    }

    @Recover
    public void recoverDelete(Exception e, String key, String bucket) {
        log.error("Failed to delete from S3 after retries: {}", e.getMessage());
        throw new StorageConnectionException("S3 delete failed", e);
    }

    /**
     * Copy an object within S3 from one location to another.
     */
    private String copy(String sourceKey, String destinationKey, String sourceBucket, String destinationBucket) {
        log.info("Copying in S3: from bucket={}/{} to bucket={}/{}",
                sourceBucket, sourceKey, destinationBucket, destinationKey);

        CopyObjectRequest copyReq = CopyObjectRequest.builder()
                .sourceBucket(sourceBucket)
                .sourceKey(sourceKey)
                .destinationBucket(destinationBucket)
                .destinationKey(destinationKey)
                .build();

        s3Client.copyObject(copyReq);
        log.info("Copied in S3: from key={} to key={}", sourceKey, destinationKey);
        return destinationKey;
    }

    /**
     * Get Storage object from Storage Service based on resource state.
     */
    private Storage getStorageForState(ResourceState state) {
        StorageType storageType = (state == ResourceState.PERMANENT) 
                ? StorageType.PERMANENT 
                : StorageType.STAGING;
        
        return storageServiceClient.getStorageByType(storageType)
                .orElseThrow(() -> new StorageConnectionException(
                        "Storage not available for state: " + state));
    }
}
