package com.epam.learn.song_service.controller;

import com.epam.learn.song_service.model.SongMetadataRequest;
import com.epam.learn.song_service.model.SongMetadataResponse;
import com.epam.learn.song_service.service.SongService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SongServiceController.class)
@DisplayName("SongServiceController Unit Tests")
class SongServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SongService songService;

    @Nested
    @DisplayName("POST /songs")
    class PostSongMetadataTests {

        @Test
        @DisplayName("Should return 200 OK with created song metadata")
        void postSongMetadata_shouldReturnOk() throws Exception {
            // Given
            String requestBody = """
                    {
                        "id": 1,
                        "name": "Test Song",
                        "artist": "Test Artist",
                        "album": "Test Album",
                        "duration": "03:00",
                        "year": "2024"
                    }
                    """;

            SongMetadataResponse response = SongMetadataResponse.builder()
                    .id(1L)
                    .name("Test Song")
                    .artist("Test Artist")
                    .album("Test Album")
                    .duration("03:00")
                    .year("2024")
                    .build();

            when(songService.saveSongMetadata(any(SongMetadataRequest.class))).thenReturn(response);

            // When & Then
            mockMvc.perform(post("/songs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Test Song"))
                    .andExpect(jsonPath("$.artist").value("Test Artist"))
                    .andExpect(jsonPath("$.album").value("Test Album"))
                    .andExpect(jsonPath("$.duration").value("03:00"))
                    .andExpect(jsonPath("$.year").value("2024"));
        }

        @Test
        @DisplayName("Should return 400 for invalid request body")
        void postSongMetadata_shouldReturn400ForInvalidBody() throws Exception {
            // Given - missing required fields
            String invalidRequest = """
                    {
                        "id": 1
                    }
                    """;

            // When & Then
            mockMvc.perform(post("/songs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /songs/{id}")
    class GetSongMetadataTests {

        @Test
        @DisplayName("Should return song metadata by resource ID")
        void getSongMetadata_shouldReturnSong() throws Exception {
            // Given
            Long resourceId = 1L;
            SongMetadataResponse response = SongMetadataResponse.builder()
                    .id(1L)
                    .name("Test Song")
                    .artist("Test Artist")
                    .album("Test Album")
                    .duration("03:00")
                    .year("2024")
                    .build();

            when(songService.getSongMetadataByResourceId(resourceId)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/songs/{id}", resourceId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Test Song"))
                    .andExpect(jsonPath("$.artist").value("Test Artist"));
        }

        @Test
        @DisplayName("Should return 404 when song not found")
        void getSongMetadata_shouldReturn404WhenNotFound() throws Exception {
            // Given
            Long resourceId = 999L;
            when(songService.getSongMetadataByResourceId(resourceId))
                    .thenThrow(new com.epam.learn.song_service.exception.NotFoundException("Not found"));

            // When & Then
            mockMvc.perform(get("/songs/{id}", resourceId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /songs")
    class DeleteSongMetadataTests {

        @Test
        @DisplayName("Should delete songs and return IDs")
        void deleteSongMetadata_shouldDeleteAndReturnIds() throws Exception {
            // Given
            String ids = "1,2,3";
            Map<String, List<Long>> response = Map.of("ids", List.of(1L, 2L, 3L));
            when(songService.deleteSongMetadata(ids)).thenReturn(response);

            // When & Then
            mockMvc.perform(delete("/songs")
                            .param("id", ids))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ids").isArray())
                    .andExpect(jsonPath("$.ids.[0]").value(1))
                    .andExpect(jsonPath("$.ids.[1]").value(2))
                    .andExpect(jsonPath("$.ids.[2]").value(3));
        }

        @Test
        @DisplayName("Should return empty list when no songs found")
        void deleteSongMetadata_shouldReturnEmptyList() throws Exception {
            // Given
            String ids = "999,998";
            Map<String, List<Long>> response = Map.of("ids", List.of());
            when(songService.deleteSongMetadata(ids)).thenReturn(response);

            // When & Then
            mockMvc.perform(delete("/songs")
                            .param("id", ids))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ids").isEmpty());
        }

        @Test
        @DisplayName("Should return 400 for invalid id format")
        void deleteSongMetadata_shouldReturn400ForInvalidFormat() throws Exception {
            // When & Then
            mockMvc.perform(delete("/songs")
                            .param("id", "abc"))
                    .andExpect(status().isBadRequest());
        }
    }
}
