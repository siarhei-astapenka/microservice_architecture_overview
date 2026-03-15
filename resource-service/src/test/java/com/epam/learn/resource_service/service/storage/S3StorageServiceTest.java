package com.epam.learn.resource_service.service.storage;

import com.epam.learn.resource_service.client.StorageServiceClient;
import com.epam.learn.resource_service.enumeration.ResourceState;
import com.epam.learn.resource_service.enumeration.StorageType;
import com.epam.learn.resource_service.exception.FileUploadException;
import com.epam.learn.resource_service.exception.StorageConnectionException;
import com.epam.learn.resource_service.model.Storage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for S3StorageService.
 * Tests S3 upload, download, and delete operations using the public API.
 */
@DisplayName("S3StorageService Unit Tests")
@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    private static final String BUCKET_NAME = "test-bucket";
    private static final String STORAGE_PATH = "resources";
    private static final String TEST_KEY = "test.mp3";
    private static final byte[] TEST_DATA = "test audio data".getBytes();

    @Mock
    private S3Client s3Client;

    @Mock
    private StorageServiceClient storageServiceClient;

    private S3StorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new S3StorageService(s3Client, storageServiceClient);
    }

    private Storage createStorage(StorageType type) {
        return Storage.builder()
                .bucket(BUCKET_NAME)
                .path(STORAGE_PATH)
                .storageType(type.getValue())
                .build();
    }

    // Positive scenarios

    @Test
    @DisplayName("Should successfully upload file to STAGING storage")
    void upload_successToStaging() {
        // Given
        Storage stagingStorage = createStorage(StorageType.STAGING);
        when(storageServiceClient.getStorageByType(StorageType.STAGING))
                .thenReturn(Optional.of(stagingStorage));
        
        PutObjectResponse mockResponse = PutObjectResponse.builder().build();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(mockResponse);

        // When
        String result = storageService.upload(TEST_DATA, TEST_KEY, ResourceState.STAGING);

        // Then
        assertNotNull(result);
        assertTrue(result.contains(TEST_KEY));
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Should successfully upload file to PERMANENT storage")
    void upload_successToPermanent() {
        // Given
        Storage permanentStorage = createStorage(StorageType.PERMANENT);
        when(storageServiceClient.getStorageByType(StorageType.PERMANENT))
                .thenReturn(Optional.of(permanentStorage));
        
        PutObjectResponse mockResponse = PutObjectResponse.builder().build();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(mockResponse);

        // When
        String result = storageService.upload(TEST_DATA, TEST_KEY, ResourceState.PERMANENT);

        // Then
        assertNotNull(result);
        assertTrue(result.contains(TEST_KEY));
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Should successfully download file from STAGING storage")
    void download_successFromStaging() {
        // Given
        Storage stagingStorage = createStorage(StorageType.STAGING);
        when(storageServiceClient.getStorageByType(StorageType.STAGING))
                .thenReturn(Optional.of(stagingStorage));
        
        byte[] expectedData = TEST_DATA;
        ResponseBytes<GetObjectResponse> mockResponse = ResponseBytes.fromByteArray(
                GetObjectResponse.builder().build(),
                expectedData
        );
        
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(mockResponse);

        // When
        byte[] result = storageService.download(TEST_KEY, ResourceState.STAGING);

        // Then
        assertArrayEquals(expectedData, result);
        verify(s3Client).getObjectAsBytes(any(GetObjectRequest.class));
    }

    @Test
    @DisplayName("Should successfully delete file from STAGING storage")
    void delete_successFromStaging() {
        // Given
        Storage stagingStorage = createStorage(StorageType.STAGING);
        when(storageServiceClient.getStorageByType(StorageType.STAGING))
                .thenReturn(Optional.of(stagingStorage));
        
        DeleteObjectResponse mockResponse = DeleteObjectResponse.builder().build();
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(mockResponse);

        // When
        storageService.delete(TEST_KEY, ResourceState.STAGING);

        // Then
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    // Negative scenarios

    @Test
    @DisplayName("Should throw FileUploadException when data is null")
    void upload_nullData() {
        // Given
        Storage stagingStorage = createStorage(StorageType.STAGING);
        when(storageServiceClient.getStorageByType(StorageType.STAGING))
                .thenReturn(Optional.of(stagingStorage));

        // When & Then
        assertThrows(FileUploadException.class, 
                () -> storageService.upload(null, TEST_KEY, ResourceState.STAGING));
    }

    @Test
    @DisplayName("Should throw FileUploadException when data is empty")
    void upload_emptyData() {
        // Given
        Storage stagingStorage = createStorage(StorageType.STAGING);
        when(storageServiceClient.getStorageByType(StorageType.STAGING))
                .thenReturn(Optional.of(stagingStorage));

        // When & Then
        assertThrows(FileUploadException.class, 
                () -> storageService.upload(new byte[0], TEST_KEY, ResourceState.STAGING));
    }

    @Test
    @DisplayName("Should throw StorageConnectionException when STAGING storage not available")
    void upload_noStagingStorage() {
        // Given
        when(storageServiceClient.getStorageByType(StorageType.STAGING))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(StorageConnectionException.class, 
                () -> storageService.upload(TEST_DATA, TEST_KEY, ResourceState.STAGING));
    }

    @Test
    @DisplayName("Should throw StorageConnectionException when PERMANENT storage not available")
    void upload_noPermanentStorage() {
        // Given
        when(storageServiceClient.getStorageByType(StorageType.PERMANENT))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(StorageConnectionException.class, 
                () -> storageService.upload(TEST_DATA, TEST_KEY, ResourceState.PERMANENT));
    }

    @Test
    @DisplayName("Should generate storage key with UUID")
    void generateStorageKey_shouldGenerateUuid() {
        // When
        String key1 = storageService.generateStorageKey();
        String key2 = storageService.generateStorageKey();

        // Then
        assertNotNull(key1);
        assertTrue(key1.endsWith(".mp3"));
        assertNotEquals(key1, key2); // Should be unique
    }

    @Test
    @DisplayName("Should construct full storage key correctly")
    void getFullStorageKey_shouldCombinePathAndKey() {
        // When
        String fullKey = storageService.getFullStorageKey(STORAGE_PATH, TEST_KEY);

        // Then
        assertEquals(STORAGE_PATH + "/" + TEST_KEY, fullKey);
    }
}
