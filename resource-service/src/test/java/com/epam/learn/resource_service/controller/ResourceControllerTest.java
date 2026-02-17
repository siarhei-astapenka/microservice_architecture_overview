package com.epam.learn.resource_service.controller;

import com.epam.learn.resource_service.service.ResourceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@WebMvcTest(ResourceController.class)
@DisplayName("ResourceController Unit Tests")
class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResourceService resourceService;

    @Nested
    @DisplayName("POST /resources")
    class UploadResourceTests {
        // Note: Upload endpoint tests are limited due to @ValidMP3 custom validator
        // which requires actual MP3 file structure. The validator is tested separately
        // in MP3FileValidatorTest, and the service layer is tested in ResourceServiceTest.
        
        @Test
        @DisplayName("Should return 400 for invalid content type")
        void uploadResource_shouldReturn400ForInvalidContentType() throws Exception {
            // Given
            byte[] fileData = "mp3 binary data".getBytes();

            // When & Then
            mockMvc.perform(post("/resources")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(fileData))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /resources/{id}")
    class DownloadResourceTests {

        @Test
        @DisplayName("Should return file with correct content type")
        void downloadResource_shouldReturnFile() throws Exception {
            // Given
            Long resourceId = 1L;
            byte[] fileData = "mp3 binary data".getBytes();
            when(resourceService.downloadResource(resourceId)).thenReturn(fileData);

            // When & Then
            mockMvc.perform(get("/resources/{id}", resourceId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.parseMediaType("audio/mpeg")))
                    .andExpect(content().bytes(fileData));
        }

        @Test
        @DisplayName("Should return 404 when resource not found")
        void downloadResource_shouldReturn404WhenNotFound() throws Exception {
            // Given
            Long resourceId = 999L;
            when(resourceService.downloadResource(resourceId))
                    .thenThrow(new com.epam.learn.resource_service.exception.NotFoundException("Resource not found"));

            // When & Then
            mockMvc.perform(get("/resources/{id}", resourceId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return correct content length")
        void downloadResource_shouldReturnCorrectContentLength() throws Exception {
            // Given
            Long resourceId = 1L;
            byte[] fileData = "test data".getBytes();
            when(resourceService.downloadResource(resourceId)).thenReturn(fileData);

            // When & Then
            MockHttpServletResponse response = mockMvc.perform(get("/resources/{id}", resourceId))
                    .andExpect(status().isOk())
                    .andReturn().getResponse();

            assertEquals(fileData.length, response.getContentLength());
        }

        @Test
        @DisplayName("Should return 400 for invalid ID (zero)")
        void downloadResource_shouldReturn400ForZeroId() throws Exception {
            // When & Then
            mockMvc.perform(get("/resources/{id}", 0))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for invalid ID (negative)")
        void downloadResource_shouldReturn400ForNegativeId() throws Exception {
            // When & Then
            mockMvc.perform(get("/resources/{id}", -1))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /resources")
    class DeleteResourcesTests {

        @Test
        @DisplayName("Should delete resources and return IDs")
        void deleteResources_shouldDeleteAndReturnIds() throws Exception {
            // Given
            String ids = "1,2,3";
            Map<String, List<Long>> expectedResponse = Map.of("ids", List.of(1L, 2L, 3L));
            when(resourceService.deleteResources(ids)).thenReturn(expectedResponse);

            // When & Then
            mockMvc.perform(delete("/resources")
                            .param("id", ids))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ids").isArray())
                    .andExpect(jsonPath("$.ids.[0]").value(1))
                    .andExpect(jsonPath("$.ids.[1]").value(2))
                    .andExpect(jsonPath("$.ids.[2]").value(3));
        }

        @Test
        @DisplayName("Should return empty list when no resources found")
        void deleteResources_shouldReturnEmptyList() throws Exception {
            // Given
            String ids = "999,998";
            Map<String, List<Long>> expectedResponse = Map.of("ids", List.of());
            when(resourceService.deleteResources(ids)).thenReturn(expectedResponse);

            // When & Then
            mockMvc.perform(delete("/resources")
                            .param("id", ids))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ids").isEmpty());
        }

        @Test
        @DisplayName("Should return 400 for invalid id format")
        void deleteResources_shouldReturn400ForInvalidFormat() throws Exception {
            // When & Then
            mockMvc.perform(delete("/resources")
                            .param("id", "abc"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle single id")
        void deleteResources_shouldHandleSingleId() throws Exception {
            // Given
            String ids = "42";
            Map<String, List<Long>> expectedResponse = Map.of("ids", List.of(42L));
            when(resourceService.deleteResources(ids)).thenReturn(expectedResponse);

            // When & Then
            mockMvc.perform(delete("/resources")
                            .param("id", ids))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ids.[0]").value(42));
        }

        @Test
        @DisplayName("Should return 400 for missing id parameter")
        void deleteResources_shouldReturn400ForMissingId() throws Exception {
            // When & Then - Missing required parameter causes 400
            mockMvc.perform(delete("/resources"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for empty id parameter")
        void deleteResources_shouldReturn400ForEmptyId() throws Exception {
            // When & Then
            mockMvc.perform(delete("/resources")
                            .param("id", ""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for id with special characters")
        void deleteResources_shouldReturn400ForSpecialCharacters() throws Exception {
            // When & Then
            mockMvc.perform(delete("/resources")
                            .param("id", "1,2;3"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for id exceeding max length")
        void deleteResources_shouldReturn400ForExceedingMaxLength() throws Exception {
            // Given - Create a CSV string with 201 IDs (exceeds 200 limit)
            StringBuilder longIds = new StringBuilder();
            for (int i = 1; i <= 201; i++) {
                if (i > 1) longIds.append(",");
                longIds.append(i);
            }

            // When & Then
            mockMvc.perform(delete("/resources")
                            .param("id", longIds.toString()))
                    .andExpect(status().isBadRequest());
        }
    }
}
