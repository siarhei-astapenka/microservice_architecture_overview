package com.epam.learn.resource_processor.service;

import com.epam.learn.resource_processor.client.ResourceServiceClient;
import com.epam.learn.resource_processor.client.SongServiceClient;
import com.epam.learn.resource_processor.dto.SongMetadataRequest;
import com.epam.learn.resource_processor.dto.SongMetadataResponse;
import com.epam.learn.resource_processor.model.metadata.Metadata;
import com.epam.learn.resource_processor.service.mapper.MetadataMapper;
import com.epam.learn.resource_processor.service.parser.MetadataParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false",
        "spring.rabbitmq.dynamic=false",
        "management.health.rabbit.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.loadbalancer.enabled=false",
        "eureka.client.enabled=false"
})
@ActiveProfiles("test")
@DisplayName("ResourceProcessorService Integration Tests")
class ResourceProcessorServiceIntegrationTest {

    @MockitoBean
    private ResourceServiceClient resourceServiceClient;

    @MockitoBean
    private SongServiceClient songServiceClient;

    @MockitoBean
    private MetadataParser metadataParser;

    @org.springframework.beans.factory.annotation.Autowired
    private ResourceProcessorService resourceProcessorService;

    @org.springframework.beans.factory.annotation.Autowired
    private MetadataMapper metadataMapper;

    @Test
    @DisplayName("processResource should orchestrate fetch-parse-map-save")
    void processResource_shouldOrchestrateAllSteps() {
        long resourceId = 123L;
        byte[] data = new byte[] {1, 2, 3};

        Metadata metadata = Metadata.builder()
                .name("Song")
                .artist("Artist")
                .album("Album")
                .duration(Duration.ofMinutes(3).plusSeconds(30))
                .year(LocalDate.of(2024, 1, 1))
                .build();

        when(resourceServiceClient.getResourceData(resourceId)).thenReturn(data);
        when(metadataParser.getSongMetadataFromFile(data)).thenReturn(Optional.of(metadata));

        SongMetadataRequest expectedRequest = metadataMapper.toSongMetadataRequest(metadata, resourceId);
        when(songServiceClient.saveSongMetadata(expectedRequest))
                .thenReturn(SongMetadataResponse.builder().id(1L).build());

        resourceProcessorService.processResource(resourceId);

        verify(resourceServiceClient).getResourceData(resourceId);
        verify(metadataParser).getSongMetadataFromFile(data);
        verify(songServiceClient).saveSongMetadata(expectedRequest);
    }

    @Test
    @DisplayName("processResource should fail when metadata is missing")
    void processResource_shouldFailWhenMetadataMissing() {
        long resourceId = 123L;
        byte[] data = new byte[] {1, 2, 3};

        when(resourceServiceClient.getResourceData(resourceId)).thenReturn(data);
        when(metadataParser.getSongMetadataFromFile(data)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> resourceProcessorService.processResource(resourceId));
        assertTrue(ex.getMessage().contains("Failed to process resource"));

        verify(songServiceClient, never()).saveSongMetadata(any());
    }
}
