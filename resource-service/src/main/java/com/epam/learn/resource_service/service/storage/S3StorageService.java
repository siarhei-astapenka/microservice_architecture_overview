package com.epam.learn.resource_service.service.storage;

import com.epam.learn.resource_service.exception.FileUploadException;
import com.epam.learn.resource_service.exception.StorageConnectionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final String bucket;

    public S3StorageService(S3Client s3Client, @Value("${s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Retryable(
            retryFor = {S3Exception.class, SdkClientException.class},
            noRetryFor = {NoSuchBucketException.class, AccessDeniedException.class, NoSuchKeyException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, maxDelay = 10000, multiplier = 2)
    )
    public String upload(byte[] data, String key) {
        if (data == null || data.length == 0) {
            throw new FileUploadException("File data must be provided");
        }

        String objectKey = key != null ? key : generateKey();
        log.info("Uploading to S3: bucket={}, key={}, size={}b", bucket, objectKey, data.length);

        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType("audio/mpeg")
                .build();

        s3Client.putObject(putReq, RequestBody.fromBytes(data));
        log.info("Uploaded to S3: key={}", objectKey);
        return objectKey;
    }

    @Retryable(
            retryFor = {S3Exception.class, SdkClientException.class},
            noRetryFor = {NoSuchKeyException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, maxDelay = 10000, multiplier = 2)
    )
    public byte[] download(String key) {
        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        ResponseBytes<?> responseBytes = s3Client.getObjectAsBytes(getReq);
        return responseBytes.asByteArray();
    }

    @Retryable(
            retryFor = {S3Exception.class, SdkClientException.class},
            noRetryFor = {NoSuchKeyException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, maxDelay = 10000, multiplier = 2)
    )
    public void delete(String key) {
        DeleteObjectRequest delReq = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        s3Client.deleteObject(delReq);
        log.info("Deleted from S3: key={}", key);
    }

    @Recover
    public String recoverUpload(Exception e, byte[] data, String key) {
        log.error("Failed to upload to S3 after retries: {}", e.getMessage());
        throw new StorageConnectionException("S3 upload failed", e);
    }

    @Recover
    public byte[] recoverDownload(Exception e, String key) {
        log.error("Failed to download from S3 after retries: {}", e.getMessage());
        throw new StorageConnectionException("S3 download failed", e);
    }

    @Recover
    public void recoverDelete(Exception e, String key) {
        log.error("Failed to delete from S3 after retries: {}", e.getMessage());
        throw new StorageConnectionException("S3 delete failed", e);
    }

    private String generateKey() {
        return "resources/" + UUID.randomUUID() + ".mp3";
    }
}