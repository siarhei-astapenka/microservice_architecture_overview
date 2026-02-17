package com.epam.learn.resource_service.controller;

import com.epam.learn.resource_service.client.SongServiceClient;
import com.epam.learn.resource_service.entity.ResourceEntity;
import com.epam.learn.resource_service.messaging.ResourceUploadProducer;
import com.epam.learn.resource_service.repository.ResourceRepository;
import com.epam.learn.resource_service.service.storage.S3StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration," +
                "org.springframework.boot.actuate.autoconfigure.amqp.RabbitHealthContributorAutoConfiguration",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.loadbalancer.enabled=false",
        "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("ResourceController Integration Tests")
class ResourceControllerIntegrationTest {

    @TestConfiguration
    static class NoRabbitInfraConfig {
        @Bean
        ConnectionFactory connectionFactory() {
            return Mockito.mock(ConnectionFactory.class);
        }
    }

    @MockitoBean
    private S3StorageService s3StorageService;

    @MockitoBean
    private ResourceUploadProducer resourceUploadProducer;

    @MockitoBean
    private SongServiceClient songServiceClient;

    @org.springframework.beans.factory.annotation.Autowired
    private MockMvc mockMvc;

    @org.springframework.beans.factory.annotation.Autowired
    private ResourceRepository resourceRepository;

    @BeforeEach
    void setUp() {
        resourceRepository.deleteAll();
        resourceRepository.flush();
    }

    @Test
    @DisplayName("POST /resources should persist resource and return id")
    void upload_shouldPersistAndReturnId() throws Exception {
        byte[] mp3;
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("testdata/valid-sample-with-required-tags.mp3")) {
            assertNotNull(is, "Missing test MP3 resource");
            mp3 = is.readAllBytes();
        }

        when(s3StorageService.upload(any(byte[].class), isNull())).thenReturn("resources/test.mp3");

        mockMvc.perform(post("/resources")
                        .contentType(MediaType.parseMediaType("audio/mpeg"))
                        .content(mp3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber());

        List<ResourceEntity> all = resourceRepository.findAll();
        assertEquals(1, all.size());
        assertEquals("resource-bucket-test", all.get(0).getStorageBucket());
        assertEquals("resources/test.mp3", all.get(0).getStorageKey());

        ArgumentCaptor<com.epam.learn.resource_service.dto.ResourceUploadMessage> captor =
                ArgumentCaptor.forClass(com.epam.learn.resource_service.dto.ResourceUploadMessage.class);
        verify(resourceUploadProducer).sendResourceUploadMessage(captor.capture());
        assertEquals(all.get(0).getId(), captor.getValue().getResourceId());
        assertEquals("resource-bucket-test", captor.getValue().getStorageBucket());
        assertEquals("resources/test.mp3", captor.getValue().getStorageKey());
    }

    @Test
    @DisplayName("POST /resources should return 400 for invalid MP3")
    void upload_shouldReturn400ForInvalidMp3() throws Exception {
        byte[] notMp3 = "not-mp3".getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(post("/resources")
                        .contentType(MediaType.parseMediaType("audio/mpeg"))
                        .content(notMp3))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("400"))
                .andExpect(jsonPath("$.errorMessage").value("Invalid file format. Only MP3 files are allowed"));
    }

    @Test
    @DisplayName("GET /resources/{id} should return bytes from storage")
    void download_shouldReturnBytes() throws Exception {
        ResourceEntity saved = resourceRepository.saveAndFlush(ResourceEntity.builder()
                .storageBucket("resource-bucket-test")
                .storageKey("k1")
                .build());

        byte[] data = new byte[]{1, 2, 3, 4};
        when(s3StorageService.download("k1")).thenReturn(data);

        mockMvc.perform(get("/resources/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "audio/mpeg"))
                .andExpect(content().bytes(data));
    }

    @Test
    @DisplayName("GET /resources/{id} should return 404 when missing")
    void download_shouldReturn404WhenMissing() throws Exception {
        mockMvc.perform(get("/resources/{id}", 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("404"))
                .andExpect(jsonPath("$.errorMessage").value("Resource not found: 9999"));
    }

    @Test
    @DisplayName("DELETE /resources should delete existing and call song-service client")
    void delete_shouldDeleteExistingAndCallSongService() throws Exception {
        ResourceEntity r1 = resourceRepository.saveAndFlush(ResourceEntity.builder()
                .storageBucket("resource-bucket-test")
                .storageKey("k1")
                .build());
        ResourceEntity r2 = resourceRepository.saveAndFlush(ResourceEntity.builder()
                .storageBucket("resource-bucket-test")
                .storageKey("k2")
                .build());

        mockMvc.perform(delete("/resources")
                        .param("id", r1.getId() + ",9999," + r2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ids").isArray());

        assertTrue(resourceRepository.findAll().isEmpty());
        verify(s3StorageService).delete("k1");
        verify(s3StorageService).delete("k2");
        verify(songServiceClient).deleteMetadata(r1.getId() + ",9999," + r2.getId());
    }

    @Test
    @DisplayName("DELETE /resources should return 400 for invalid id format")
    void delete_shouldReturn400ForInvalidIdFormat() throws Exception {
        mockMvc.perform(delete("/resources")
                        .param("id", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("400"))
                .andExpect(jsonPath("$.errorMessage").value("'id' must be comma-separated numbers or single number"));
    }
}
