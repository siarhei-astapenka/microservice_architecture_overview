package com.epam.learn.resource_service.contract;

import com.epam.learn.resource_service.ResourceServiceApplication;
import com.epam.learn.resource_service.controller.ResourceController;
import com.epam.learn.resource_service.service.ResourceService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Base class for Spring Cloud Contract producer tests in resource-service (HTTP contracts).
 * Provides MockMvc setup and mock stubs for the ResourceService.
 */
@WebMvcTest(controllers = ResourceController.class)
@ContextConfiguration(classes = ResourceServiceApplication.class)
@ActiveProfiles("test")
public abstract class ResourceServiceHttpContractBase {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResourceService resourceService;

    @BeforeEach
    public void setup() {
        RestAssuredMockMvc.mockMvc(mockMvc);

        // Stub for GET /resources/{id} - download resource binary data
        byte[] sampleMp3 = new byte[]{0x49, 0x44, 0x33}; // minimal MP3 header bytes
        when(resourceService.downloadResource(anyLong())).thenReturn(sampleMp3);

        // Stub for DELETE /resources - delete resources
        when(resourceService.deleteResources(anyString())).thenReturn(Map.of("ids", List.of(1L)));

        // Stub for POST /resources - upload resource
        when(resourceService.uploadResource(org.mockito.ArgumentMatchers.any(byte[].class)))
                .thenReturn(Map.of("id", 1L));
    }
}
