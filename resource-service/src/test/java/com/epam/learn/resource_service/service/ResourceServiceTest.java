package com.epam.learn.resource_service.service;

import com.epam.learn.resource_service.client.SongServiceClient;
import com.epam.learn.resource_service.entity.ResourceEntity;
import com.epam.learn.resource_service.messaging.ResourceUploadProducer;
import com.epam.learn.resource_service.repository.ResourceRepository;
import com.epam.learn.resource_service.service.storage.S3StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceService Unit Tests")
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private S3StorageService s3StorageService;

    @Mock
    private ResourceUploadProducer resourceUploadProducer;

    @Mock
    private SongServiceClient songServiceClient;

    @InjectMocks
    private ResourceService resourceService;

    private static final String S3_BUCKET = "test-bucket";

    @Nested
    @DisplayName("uploadResource")
    class UploadResourceTests {

        @Test
        @DisplayName("Should upload resource successfully")
        void uploadResource_shouldUploadSuccessfully() {
            // Given
            byte[] fileData = "test mp3 data".getBytes();
            String s3Key = "resources/test-key.mp3";
            ResourceEntity savedEntity = ResourceEntity.builder()
                    .id(1L)
                    .storageBucket(S3_BUCKET)
                    .storageKey(s3Key)
                    .build();

            when(s3StorageService.upload(any(byte[].class), isNull())).thenReturn(s3Key);
            when(resourceRepository.save(any(ResourceEntity.class))).thenReturn(savedEntity);
            doNothing().when(resourceUploadProducer).sendResourceUploadMessage(any());

            // When
            Map<String, Long> result = resourceService.uploadResource(fileData);

            // Then
            assertNotNull(result);
            assertEquals(1L, result.get("id"));
            verify(s3StorageService).upload(eq(fileData), isNull());
            verify(resourceRepository).save(any(ResourceEntity.class));
            verify(resourceUploadProducer).sendResourceUploadMessage(any());
        }

        @Test
        @DisplayName("Should handle message sending failure gracefully")
        void uploadResource_shouldHandleMessageFailure() {
            // Given
            byte[] fileData = "test mp3 data".getBytes();
            String s3Key = "resources/test-key.mp3";
            ResourceEntity savedEntity = ResourceEntity.builder()
                    .id(1L)
                    .storageBucket(S3_BUCKET)
                    .storageKey(s3Key)
                    .build();

            when(s3StorageService.upload(any(byte[].class), isNull())).thenReturn(s3Key);
            when(resourceRepository.save(any(ResourceEntity.class))).thenReturn(savedEntity);
            doThrow(new RuntimeException("RabbitMQ down"))
                    .when(resourceUploadProducer).sendResourceUploadMessage(any());

            // When
            Map<String, Long> result = resourceService.uploadResource(fileData);

            // Then
            assertNotNull(result);
            assertEquals(1L, result.get("id"));
            // Should not throw, message failure is handled gracefully
        }

        @Test
        @DisplayName("Should return correct resource ID")
        void uploadResource_shouldReturnCorrectId() {
            // Given
            byte[] fileData = "test data".getBytes();
            Long expectedId = 42L;
            ResourceEntity savedEntity = ResourceEntity.builder()
                    .id(expectedId)
                    .storageBucket(S3_BUCKET)
                    .storageKey("key")
                    .build();

            when(s3StorageService.upload(any(byte[].class), isNull())).thenReturn("key");
            when(resourceRepository.save(any(ResourceEntity.class))).thenReturn(savedEntity);
            doNothing().when(resourceUploadProducer).sendResourceUploadMessage(any());

            // When
            Map<String, Long> result = resourceService.uploadResource(fileData);

            // Then
            assertEquals(expectedId, result.get("id"));
        }
    }

    @Nested
    @DisplayName("downloadResource")
    class DownloadResourceTests {

        @Test
        @DisplayName("Should download resource successfully")
        void downloadResource_shouldDownloadSuccessfully() {
            // Given
            Long resourceId = 1L;
            byte[] expectedData = "mp3 content".getBytes();
            ResourceEntity entity = ResourceEntity.builder()
                    .id(resourceId)
                    .storageBucket(S3_BUCKET)
                    .storageKey("test-key.mp3")
                    .build();

            when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(entity));
            when(s3StorageService.download("test-key.mp3")).thenReturn(expectedData);

            // When
            byte[] result = resourceService.downloadResource(resourceId);

            // Then
            assertNotNull(result);
            assertEquals(expectedData.length, result.length);
            verify(resourceRepository).findById(resourceId);
            verify(s3StorageService).download("test-key.mp3");
        }

        @Test
        @DisplayName("Should throw NotFoundException when resource not found")
        void downloadResource_shouldThrowNotFoundException() {
            // Given
            Long resourceId = 999L;
            when(resourceRepository.findById(resourceId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(com.epam.learn.resource_service.exception.NotFoundException.class,
                    () -> resourceService.downloadResource(resourceId));
        }
    }

    @Nested
    @DisplayName("deleteResources")
    class DeleteResourcesTests {

        @Test
        @DisplayName("Should delete resources successfully")
        void deleteResources_shouldDeleteSuccessfully() {
            // Given
            String ids = "1,2,3";
            List<Long> existingIds = List.of(1L, 2L, 3L);
            List<ResourceEntity> entities = List.of(
                    ResourceEntity.builder().id(1L).storageKey("key1").build(),
                    ResourceEntity.builder().id(2L).storageKey("key2").build(),
                    ResourceEntity.builder().id(3L).storageKey("key3").build()
            );

            when(resourceRepository.findExistingIds(anyList())).thenReturn(existingIds);
            when(resourceRepository.findById(1L)).thenReturn(Optional.of(entities.get(0)));
            when(resourceRepository.findById(2L)).thenReturn(Optional.of(entities.get(1)));
            when(resourceRepository.findById(3L)).thenReturn(Optional.of(entities.get(2)));
            when(songServiceClient.deleteMetadata(ids)).thenReturn(Map.of("ids", existingIds));
            doNothing().when(s3StorageService).delete(anyString());
            doNothing().when(resourceRepository).deleteAllById(anyList());

            // When
            Map<String, List<Long>> result = resourceService.deleteResources(ids);

            // Then
            assertNotNull(result);
            assertEquals(3, result.get("ids").size());
            assertTrue(result.get("ids").containsAll(existingIds));
            verify(s3StorageService).delete("key1");
            verify(s3StorageService).delete("key2");
            verify(s3StorageService).delete("key3");
            verify(resourceRepository).deleteAllById(existingIds);
            verify(songServiceClient).deleteMetadata(ids);
        }

        @Test
        @DisplayName("Should handle empty ids gracefully")
        void deleteResources_shouldHandleEmptyIds() {
            // Given
            String ids = "999,998"; // Non-empty but non-existent
            when(resourceRepository.findExistingIds(anyList())).thenReturn(Collections.emptyList());
            when(songServiceClient.deleteMetadata(ids)).thenReturn(Map.of("ids", List.of()));

            // When
            Map<String, List<Long>> result = resourceService.deleteResources(ids);

            // Then
            assertNotNull(result);
            assertTrue(result.get("ids").isEmpty());
            // Note: songServiceClient is still called in the service, even when no resources found
        }

        @Test
        @DisplayName("Should handle song service failure gracefully")
        void deleteResources_shouldHandleSongServiceFailure() {
            // Given
            String ids = "1,2";
            List<Long> existingIds = List.of(1L, 2L);
            ResourceEntity entity = ResourceEntity.builder()
                    .id(1L)
                    .storageKey("key1")
                    .build();

            when(resourceRepository.findExistingIds(anyList())).thenReturn(existingIds);
            when(resourceRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(resourceRepository.findById(2L)).thenReturn(Optional.of(entity));
            when(songServiceClient.deleteMetadata(ids)).thenThrow(new RuntimeException("Service unavailable"));
            doNothing().when(s3StorageService).delete(anyString());
            doNothing().when(resourceRepository).deleteAllById(anyList());

            // When
            Map<String, List<Long>> result = resourceService.deleteResources(ids);

            // Then
            assertNotNull(result);
            // Should still delete from local resources even if song-service fails
            verify(resourceRepository).deleteAllById(existingIds);
        }

        @Test
        @DisplayName("Should delete only existing resources")
        void deleteResources_shouldDeleteOnlyExisting() {
            // Given
            String ids = "1,999,3"; // 999 doesn't exist
            List<Long> existingIds = List.of(1L, 3L);
            List<ResourceEntity> entities = List.of(
                    ResourceEntity.builder().id(1L).storageKey("key1").build(),
                    ResourceEntity.builder().id(3L).storageKey("key3").build()
            );

            when(resourceRepository.findExistingIds(anyList())).thenReturn(existingIds);
            when(resourceRepository.findById(1L)).thenReturn(Optional.of(entities.get(0)));
            when(resourceRepository.findById(3L)).thenReturn(Optional.of(entities.get(1)));
            when(songServiceClient.deleteMetadata(ids)).thenReturn(Map.of("ids", existingIds));
            doNothing().when(s3StorageService).delete(anyString());
            doNothing().when(resourceRepository).deleteAllById(anyList());

            // When
            Map<String, List<Long>> result = resourceService.deleteResources(ids);

            // Then
            assertNotNull(result);
            assertEquals(2, result.get("ids").size());
            assertFalse(result.get("ids").contains(999L));
        }

        @Test
        @DisplayName("Should parse single id correctly")
        void deleteResources_shouldParseSingleId() {
            // Given
            String ids = "42";
            List<Long> existingIds = List.of(42L);
            ResourceEntity entity = ResourceEntity.builder()
                    .id(42L)
                    .storageKey("key42")
                    .build();

            when(resourceRepository.findExistingIds(anyList())).thenReturn(existingIds);
            when(resourceRepository.findById(42L)).thenReturn(Optional.of(entity));
            when(songServiceClient.deleteMetadata(ids)).thenReturn(Map.of("ids", existingIds));
            doNothing().when(s3StorageService).delete(anyString());
            doNothing().when(resourceRepository).deleteAllById(anyList());

            // When
            Map<String, List<Long>> result = resourceService.deleteResources(ids);

            // Then
            assertNotNull(result);
            assertEquals(1, result.get("ids").size());
            assertTrue(result.get("ids").contains(42L));
        }
    }

    @Nested
    @DisplayName("saveResourceEntity")
    class SaveResourceEntityTests {

        @Test
        @DisplayName("Should save resource entity with correct bucket and key")
        void saveResourceEntity_shouldSaveCorrectly() {
            // Given
            String s3Key = "resources/test.mp3";
            ResourceEntity savedEntity = ResourceEntity.builder()
                    .id(1L)
                    .storageBucket(S3_BUCKET)
                    .storageKey(s3Key)
                    .build();

            when(resourceRepository.save(any(ResourceEntity.class))).thenReturn(savedEntity);

            // When
            ResourceEntity result = resourceService.saveResourceEntity(s3Key);

            // Then
            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals(S3_BUCKET, result.getStorageBucket());
            assertEquals(s3Key, result.getStorageKey());
        }
    }
}
