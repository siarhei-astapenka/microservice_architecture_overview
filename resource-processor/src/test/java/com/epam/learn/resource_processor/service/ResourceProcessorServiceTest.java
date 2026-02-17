package com.epam.learn.resource_processor.service;

import com.epam.learn.resource_processor.client.ResourceServiceClient;
import com.epam.learn.resource_processor.client.SongServiceClient;
import com.epam.learn.resource_processor.dto.SongMetadataRequest;
import com.epam.learn.resource_processor.dto.SongMetadataResponse;
import com.epam.learn.resource_processor.model.metadata.Metadata;
import com.epam.learn.resource_processor.service.mapper.MetadataMapper;
import com.epam.learn.resource_processor.service.parser.MetadataParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceProcessorService Unit Tests")
class ResourceProcessorServiceTest {

    @Mock
    private ResourceServiceClient resourceServiceClient;

    @Mock
    private SongServiceClient songServiceClient;

    @Mock
    private MetadataParser metadataParser;

    @Mock
    private MetadataMapper metadataMapper;

    @InjectMocks
    private ResourceProcessorService resourceProcessorService;

    @Nested
    @DisplayName("processResource")
    class ProcessResourceTests {

        @Test
        @DisplayName("Should process resource successfully")
        void processResource_shouldProcessSuccessfully() {
            // Given
            Long resourceId = 1L;
            byte[] fileData = "test mp3 data".getBytes();
            Metadata metadata = Metadata.builder()
                    .name("Test Song")
                    .artist("Test Artist")
                    .album("Test Album")
                    .duration(Duration.ofMinutes(3))
                    .year(java.time.LocalDate.of(2024, 1, 1))
                    .build();
            SongMetadataRequest metadataRequest = SongMetadataRequest.builder()
                    .id(resourceId)
                    .name("Test Song")
                    .artist("Test Artist")
                    .album("Test Album")
                    .duration("03:00")
                    .year("2024")
                    .build();
            SongMetadataResponse metadataResponse = SongMetadataResponse.builder()
                    .id(1L)
                    .build();

            when(resourceServiceClient.getResourceData(resourceId)).thenReturn(fileData);
            when(metadataParser.getSongMetadataFromFile(fileData)).thenReturn(java.util.Optional.of(metadata));
            when(metadataMapper.toSongMetadataRequest(metadata, resourceId)).thenReturn(metadataRequest);
            when(songServiceClient.saveSongMetadata(metadataRequest)).thenReturn(metadataResponse);

            // When
            assertDoesNotThrow(() -> resourceProcessorService.processResource(resourceId));

            // Then
            verify(resourceServiceClient).getResourceData(resourceId);
            verify(metadataParser).getSongMetadataFromFile(fileData);
            verify(metadataMapper).toSongMetadataRequest(metadata, resourceId);
            verify(songServiceClient).saveSongMetadata(metadataRequest);
        }

        @Test
        @DisplayName("Should throw RuntimeException when no metadata found")
        void processResource_shouldThrowWhenNoMetadata() {
            // Given
            Long resourceId = 1L;
            byte[] fileData = "test data".getBytes();

            when(resourceServiceClient.getResourceData(resourceId)).thenReturn(fileData);
            when(metadataParser.getSongMetadataFromFile(fileData)).thenReturn(java.util.Optional.empty());

            // When & Then
            assertThrows(RuntimeException.class, () -> resourceProcessorService.processResource(resourceId));
        }

        @Test
        @DisplayName("Should propagate exception when resource service fails")
        void processResource_shouldPropagateResourceServiceException() {
            // Given
            Long resourceId = 1L;
            when(resourceServiceClient.getResourceData(resourceId))
                    .thenThrow(new RuntimeException("Resource service unavailable"));

            // When & Then
            assertThrows(RuntimeException.class, () -> resourceProcessorService.processResource(resourceId));
        }

        @Test
        @DisplayName("Should propagate exception when song service fails")
        void processResource_shouldPropagateSongServiceException() {
            // Given
            Long resourceId = 1L;
            byte[] fileData = "test data".getBytes();
            Metadata metadata = Metadata.builder()
                    .name("Test Song")
                    .artist("Test Artist")
                    .album("Test Album")
                    .duration(Duration.ofMinutes(3))
                    .year(java.time.LocalDate.of(2024, 1, 1))
                    .build();
            SongMetadataRequest metadataRequest = SongMetadataRequest.builder()
                    .id(resourceId)
                    .name("Test Song")
                    .build();

            when(resourceServiceClient.getResourceData(resourceId)).thenReturn(fileData);
            when(metadataParser.getSongMetadataFromFile(fileData)).thenReturn(java.util.Optional.of(metadata));
            when(metadataMapper.toSongMetadataRequest(metadata, resourceId)).thenReturn(metadataRequest);
            when(songServiceClient.saveSongMetadata(metadataRequest))
                    .thenThrow(new RuntimeException("Song service unavailable"));

            // When & Then
            assertThrows(RuntimeException.class, () -> resourceProcessorService.processResource(resourceId));
        }

        @Test
        @DisplayName("Should handle empty file data")
        void processResource_shouldHandleEmptyFile() {
            // Given
            Long resourceId = 1L;
            byte[] emptyData = new byte[0];

            when(resourceServiceClient.getResourceData(resourceId)).thenReturn(emptyData);
            when(metadataParser.getSongMetadataFromFile(emptyData)).thenReturn(java.util.Optional.empty());

            // When & Then
            assertThrows(RuntimeException.class, () -> resourceProcessorService.processResource(resourceId));
        }
    }
}
