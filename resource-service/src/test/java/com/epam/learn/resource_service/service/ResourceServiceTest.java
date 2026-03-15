package com.epam.learn.resource_service.service;

import com.epam.learn.resource_service.client.SongServiceClient;
import com.epam.learn.resource_service.client.StorageServiceClient;
import com.epam.learn.resource_service.entity.ResourceEntity;
import com.epam.learn.resource_service.enumeration.ResourceState;
import com.epam.learn.resource_service.enumeration.StorageType;
import com.epam.learn.resource_service.messaging.ResourceUploadProducer;
import com.epam.learn.resource_service.model.Storage;
import com.epam.learn.resource_service.repository.ResourceRepository;
import com.epam.learn.resource_service.service.storage.S3StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Mock
    private StorageServiceClient storageServiceClient;

    @InjectMocks
    private ResourceService resourceService;

    private Storage createStorage(StorageType type) {
        return Storage.builder()
                .bucket("test-bucket")
                .path("resources")
                .storageType(type.getValue())
                .build();
    }

    @Nested
    @DisplayName("uploadResource")
    class UploadResourceTests {

        @Test
        @DisplayName("Should upload resource successfully")
        void uploadResource_shouldUploadSuccessfully() {
            // Given
            byte[] fileData = "test mp3 data".getBytes();
            String s3Key = "test-key.mp3";
            ResourceEntity savedEntity = ResourceEntity.builder()
                    .id(1L)
                    .storageKey(s3Key)
                    .state(ResourceState.STAGING)
                    .build();

            when(s3StorageService.generateStorageKey()).thenReturn(s3Key);
            when(s3StorageService.upload(any(byte[].class), eq(s3Key), eq(ResourceState.STAGING))).thenReturn(s3Key);
            when(resourceRepository.save(any(ResourceEntity.class))).thenReturn(savedEntity);
            
            Storage stagingStorage = createStorage(StorageType.STAGING);
            when(storageServiceClient.getStorageByType(StorageType.STAGING))
                    .thenReturn(Optional.of(stagingStorage));
            
            doNothing().when(resourceUploadProducer).sendResourceUploadMessage(any());

            // When
            Map<String, Long> result = resourceService.uploadResource(fileData);

            // Then
            assertNotNull(result);
            assertEquals(1L, result.get("id"));
            verify(s3StorageService).upload(eq(fileData), eq(s3Key), eq(ResourceState.STAGING));
            verify(resourceRepository).save(any(ResourceEntity.class));
            verify(resourceUploadProducer).sendResourceUploadMessage(any());
        }

        @Test
        @DisplayName("Should handle message sending failure gracefully")
        void uploadResource_shouldHandleMessageFailure() {
            // Given
            byte[] fileData = "test mp3 data".getBytes();
            String s3Key = "test-key.mp3";
            ResourceEntity savedEntity = ResourceEntity.builder()
                    .id(1L)
                    .storageKey(s3Key)
                    .state(ResourceState.STAGING)
                    .build();

            when(s3StorageService.generateStorageKey()).thenReturn(s3Key);
            when(s3StorageService.upload(any(byte[].class), eq(s3Key), eq(ResourceState.STAGING))).thenReturn(s3Key);
            when(resourceRepository.save(any(ResourceEntity.class))).thenReturn(savedEntity);
            
            Storage stagingStorage = createStorage(StorageType.STAGING);
            when(storageServiceClient.getStorageByType(StorageType.STAGING))
                    .thenReturn(Optional.of(stagingStorage));
            
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
                    .storageKey("key")
                    .state(ResourceState.STAGING)
                    .build();

            when(s3StorageService.generateStorageKey()).thenReturn("key");
            when(s3StorageService.upload(any(byte[].class), eq("key"), eq(ResourceState.STAGING))).thenReturn("key");
            when(resourceRepository.save(any(ResourceEntity.class))).thenReturn(savedEntity);
            
            Storage stagingStorage = createStorage(StorageType.STAGING);
            when(storageServiceClient.getStorageByType(StorageType.STAGING))
                    .thenReturn(Optional.of(stagingStorage));
            
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
                    .storageKey("test-key.mp3")
                    .state(ResourceState.STAGING)
                    .build();

            when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(entity));
            when(s3StorageService.download("test-key.mp3", ResourceState.STAGING)).thenReturn(expectedData);

            // When
            byte[] result = resourceService.downloadResource(resourceId);

            // Then
            assertNotNull(result);
            assertEquals(expectedData.length, result.length);
            verify(resourceRepository).findById(resourceId);
            verify(s3StorageService).download("test-key.mp3", ResourceState.STAGING);
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
                    ResourceEntity.builder().id(1L).storageKey("key1").state(ResourceState.STAGING).build(),
                    ResourceEntity.builder().id(2L).storageKey("key2").state(ResourceState.STAGING).build(),
                    ResourceEntity.builder().id(3L).storageKey("key3").state(ResourceState.STAGING).build()
            );

            when(resourceRepository.findExistingIds(anyList())).thenReturn(existingIds);
            when(resourceRepository.findById(1L)).thenReturn(Optional.of(entities.get(0)));
            when(resourceRepository.findById(2L)).thenReturn(Optional.of(entities.get(1)));
            when(resourceRepository.findById(3L)).thenReturn(Optional.of(entities.get(2)));
            when(songServiceClient.deleteMetadata(ids)).thenReturn(Map.of("ids", existingIds));
            doNothing().when(s3StorageService).delete(anyString(), any(ResourceState.class));
            doNothing().when(resourceRepository).deleteAllById(anyList());

            // When
            Map<String, List<Long>> result = resourceService.deleteResources(ids);

            // Then
            assertNotNull(result);
            assertEquals(3, result.get("ids").size());
            assertTrue(result.get("ids").containsAll(existingIds));
            verify(s3StorageService).delete("key1", ResourceState.STAGING);
            verify(s3StorageService).delete("key2", ResourceState.STAGING);
            verify(s3StorageService).delete("key3", ResourceState.STAGING);
            verify(resourceRepository).deleteAllById(existingIds);
            verify(songServiceClient).deleteMetadata(ids);
        }

        @Test
        @DisplayName("Should delete resources with PERMANENT state")
        void deleteResources_shouldDeletePermanentResources() {
            // Given
            String ids = "1";
            List<Long> existingIds = List.of(1L);
            ResourceEntity entity = ResourceEntity.builder()
                    .id(1L)
                    .storageKey("key1")
                    .state(ResourceState.PERMANENT)
                    .build();

            when(resourceRepository.findExistingIds(anyList())).thenReturn(existingIds);
            when(resourceRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(songServiceClient.deleteMetadata(ids)).thenReturn(Map.of("ids", existingIds));
            doNothing().when(s3StorageService).delete(anyString(), any(ResourceState.class));
            doNothing().when(resourceRepository).deleteAllById(anyList());

            // When
            Map<String, List<Long>> result = resourceService.deleteResources(ids);

            // Then
            assertNotNull(result);
            assertEquals(1, result.get("ids").size());
            verify(s3StorageService).delete("key1", ResourceState.PERMANENT);
        }
    }

    @Nested
    @DisplayName("handleProcessingComplete")
    class HandleProcessingCompleteTests {

        @Test
        @DisplayName("Should move resource from STAGING to PERMANENT")
        void handleProcessingComplete_shouldMoveToPermanent() {
            // Given
            Long resourceId = 1L;
            ResourceEntity entity = ResourceEntity.builder()
                    .id(resourceId)
                    .storageKey("old-key.mp3")
                    .state(ResourceState.STAGING)
                    .build();

            when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(entity));
            when(s3StorageService.move(anyString(), anyString())).thenReturn("new-key.mp3");
            when(resourceRepository.save(any(ResourceEntity.class))).thenReturn(entity);

            // When
            resourceService.handleProcessingComplete(resourceId);

            // Then
            verify(s3StorageService).move("old-key.mp3", anyString());
            verify(resourceRepository).save(argThat(e -> e.getState() == ResourceState.PERMANENT));
        }

        @Test
        @DisplayName("Should skip if resource is not in STAGING state")
        void handleProcessingComplete_shouldSkipIfNotStaging() {
            // Given
            Long resourceId = 1L;
            ResourceEntity entity = ResourceEntity.builder()
                    .id(resourceId)
                    .storageKey("key.mp3")
                    .state(ResourceState.PERMANENT)
                    .build();

            when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(entity));

            // When
            resourceService.handleProcessingComplete(resourceId);

            // Then
            verify(s3StorageService, never()).move(anyString(), anyString());
            verify(resourceRepository, never()).save(any());
        }
    }
}
