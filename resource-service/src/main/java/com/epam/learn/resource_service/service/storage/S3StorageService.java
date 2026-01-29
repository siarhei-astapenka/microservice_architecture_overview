package com.epam.learn.resource_service.service.storage;

import com.epam.learn.resource_service.exception.FileUploadException;
import com.epam.learn.resource_service.exception.StorageConnectionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.UUID;

@Service
public class S3StorageService {

    private final S3Client s3Client;
    private final String bucket;

    public S3StorageService(S3Client s3Client, @Value("${s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    public String upload(byte[] data, String key) {
        String objectKey = key != null ? key : generateKey();

        try {
            PutObjectRequest putReq = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType("audio/mpeg")
                    .build();

            s3Client.putObject(putReq, RequestBody.fromBytes(data));

            return objectKey;
        } catch (S3Exception e) {
            handleS3Exception(e);
            throw new FileUploadException("Failed to upload file to storage", e);
        } catch (SdkClientException e) {
            throw new StorageConnectionException("Unable to connect to storage service", e);
        }
    }

    public byte[] download(String key) {
        try {
            GetObjectRequest getReq = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            ResponseBytes<?> responseBytes = s3Client.getObjectAsBytes(getReq);

            return responseBytes.asByteArray();
        } catch (NoSuchKeyException e) {
            throw new FileUploadException("File not found with key: " + key, e);
        } catch (S3Exception e) {
            handleS3Exception(e);
            throw new FileUploadException("Failed to download file from storage", e);
        } catch (SdkClientException e) {
            throw new StorageConnectionException("Unable to connect to storage service", e);
        }
    }

    public void delete(String key) {
        try {
            DeleteObjectRequest delReq = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(delReq);
        } catch (NoSuchKeyException e) {
            // File already deleted or doesn't exist - это нормально для delete операции
            throw new FileUploadException("File not found for deletion: " + key, e);
        } catch (S3Exception e) {
            handleS3Exception(e);
            throw new FileUploadException("Failed to delete file from storage", e);
        } catch (SdkClientException e) {
            throw new StorageConnectionException("Unable to connect to storage service", e);
        }
    }

    private void handleS3Exception(S3Exception e) {
        String errorCode = e.awsErrorDetails().errorCode();
        String errorMessage = e.awsErrorDetails().errorMessage();

        switch (errorCode) {
            case "NoSuchBucket":
                throw new FileUploadException("Storage bucket does not exist: " + bucket, e);
            case "AccessDenied":
                throw new FileUploadException("Access denied to storage bucket", e);
            case "InvalidAccessKeyId":
            case "SignatureDoesNotMatch":
                throw new FileUploadException("Invalid storage credentials", e);
            case "RequestTimeout":
                throw new StorageConnectionException("Storage request timeout", e);
            default:
                throw new FileUploadException(
                        String.format("Storage error [%s]: %s", errorCode, errorMessage),
                        e
                );
        }
    }

    private String generateKey() {
        return "resources/" + UUID.randomUUID() + ".mp3";
    }
}
