package com.epam.learn.resource_service.service.storage;

import com.epam.learn.resource_service.exception.FileUploadException;
import com.epam.learn.resource_service.exception.StorageConnectionException;
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for S3StorageService.
 * Tests S3 upload, download, and delete operations.
 */
@DisplayName("S3StorageService Unit Tests")
@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    private static final String BUCKET_NAME = "test-bucket";
    private static final String TEST_KEY = "resources/test.mp3";
    private static final byte[] TEST_DATA = "test audio data".getBytes();

    @Mock
    private S3Client s3Client;

    private S3StorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new S3StorageService(s3Client, BUCKET_NAME);
    }

    // Positive scenarios

    @Test
    @DisplayName("Should successfully upload file with provided key")
    void upload_successWithProvidedKey() {
        // Given
        String expectedKey = "resources/uploaded.mp3";
        PutObjectResponse mockResponse = PutObjectResponse.builder().build();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(mockResponse);

        // When
        String result = storageService.upload(TEST_DATA, expectedKey);

        // Then
        assertEquals(expectedKey, result);
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Should successfully upload file with generated key")
    void upload_successWithGeneratedKey() {
        // Given
        PutObjectResponse mockResponse = PutObjectResponse.builder().build();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(mockResponse);

        // When
        String result = storageService.upload(TEST_DATA, null);

        // Then
        assertNotNull(result);
        assertTrue(result.startsWith("resources/"));
        assertTrue(result.endsWith(".mp3"));
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Should successfully download file")
    void download_success() {
        // Given
        byte[] expectedData = TEST_DATA;
        ResponseBytes<GetObjectResponse> mockResponse = ResponseBytes.fromByteArray(
                GetObjectResponse.builder().build(),
                expectedData
        );
        
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(mockResponse);

        // When
        byte[] result = storageService.download(TEST_KEY);

        // Then
        assertArrayEquals(expectedData, result);
        verify(s3Client).getObjectAsBytes(any(GetObjectRequest.class));
    }

    @Test
    @DisplayName("Should successfully delete file")
    void delete_success() {
        // Given
        DeleteObjectResponse mockResponse = DeleteObjectResponse.builder().build();
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(mockResponse);

        // When
        storageService.delete(TEST_KEY);

        // Then
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    // Negative scenarios

    @Test
    @DisplayName("Should throw FileUploadException when data is null")
    void upload_nullData() {
        // When & Then
        assertThrows(FileUploadException.class, () -> storageService.upload(null, TEST_KEY));
    }

    @Test
    @DisplayName("Should throw FileUploadException when data is empty")
    void upload_emptyData() {
        // When & Then
        assertThrows(FileUploadException.class, () -> storageService.upload(new byte[0], TEST_KEY));
    }

    @Test
    @DisplayName("Should recover from upload failure")
    void recoverUpload_shouldThrowStorageConnectionException() {
        // Given
        Exception exception = new RuntimeException("Upload failed");

        // When & Then
        assertThrows(StorageConnectionException.class,
                () -> storageService.recoverUpload(exception, TEST_DATA, TEST_KEY));
    }

    @Test
    @DisplayName("Should recover from download failure")
    void recoverDownload_shouldThrowStorageConnectionException() {
        // Given
        Exception exception = new RuntimeException("Download failed");

        // When & Then
        assertThrows(StorageConnectionException.class,
                () -> storageService.recoverDownload(exception, TEST_KEY));
    }

    @Test
    @DisplayName("Should recover from delete failure")
    void recoverDelete_shouldThrowStorageConnectionException() {
        // Given
        Exception exception = new RuntimeException("Delete failed");

        // When & Then
        assertThrows(StorageConnectionException.class,
                () -> storageService.recoverDelete(exception, TEST_KEY));
    }
}
