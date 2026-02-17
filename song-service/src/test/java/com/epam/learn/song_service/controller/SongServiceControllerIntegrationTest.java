package com.epam.learn.song_service.controller;

import com.epam.learn.song_service.entity.SongEntity;
import com.epam.learn.song_service.repository.SongRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("SongServiceController Integration Tests")
class SongServiceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SongRepository songRepository;

    @BeforeEach
    void setUp() {
        songRepository.deleteAll();
        songRepository.flush();
    }

    @Test
    @DisplayName("POST /songs should persist metadata and return DB id")
    void postSongs_shouldPersistAndReturnId() throws Exception {
        String requestBody = """
                {
                    "id": 100,
                    "name": "Test Song",
                    "artist": "Test Artist",
                    "album": "Test Album",
                    "duration": "03:30",
                    "year": "2024"
                }
                """;

        MvcResult result = mockMvc.perform(post("/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        long returnedId = json.get("id").asLong();

        SongEntity persisted = songRepository.findByResourceId(100L).orElseThrow();
        assertEquals(persisted.getId(), returnedId);
        assertEquals("Test Song", persisted.getName());
        assertEquals("Test Artist", persisted.getArtist());
        assertEquals("Test Album", persisted.getAlbum());
        assertEquals(Duration.ofMinutes(3).plusSeconds(30), persisted.getDuration());
        assertEquals(LocalDate.of(2024, 1, 1), persisted.getYear());
    }

    @Test
    @DisplayName("POST /songs should return 409 when resourceId already exists")
    void postSongs_shouldReturn409OnConflict() throws Exception {
        songRepository.saveAndFlush(SongEntity.builder()
                .resourceId(1L)
                .name("Existing")
                .artist("Existing")
                .album("Existing")
                .duration(Duration.ofSeconds(30))
                .year(LocalDate.of(2020, 1, 1))
                .build());

        String requestBody = """
                {
                    "id": 1,
                    "name": "New Song",
                    "artist": "New Artist",
                    "album": "New Album",
                    "duration": "00:30",
                    "year": "2020"
                }
                """;

        mockMvc.perform(post("/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("409"))
                .andExpect(jsonPath("$.errorMessage").value("Metadata for resource ID=1 already exists"));
    }

    @Test
    @DisplayName("POST /songs should return 400 for invalid body")
    void postSongs_shouldReturn400ForInvalidBody() throws Exception {
        String invalidRequest = """
                {
                    "id": 1
                }
                """;

        mockMvc.perform(post("/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("400"))
                .andExpect(jsonPath("$.errorMessage").value("Validation error"))
                .andExpect(jsonPath("$.details").exists());
    }

    @Test
    @DisplayName("GET /songs/{id} should return song metadata by resourceId")
    void getSongs_shouldReturnMetadata() throws Exception {
        songRepository.saveAndFlush(SongEntity.builder()
                .resourceId(200L)
                .name("Test Song")
                .artist("Test Artist")
                .album("Test Album")
                .duration(Duration.ofMinutes(3).plusSeconds(30))
                .year(LocalDate.of(2024, 1, 1))
                .build());

        mockMvc.perform(get("/songs/{id}", 200L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(200))
                .andExpect(jsonPath("$.name").value("Test Song"))
                .andExpect(jsonPath("$.artist").value("Test Artist"))
                .andExpect(jsonPath("$.album").value("Test Album"))
                .andExpect(jsonPath("$.duration").value("03:30"))
                .andExpect(jsonPath("$.year").value("2024"));
    }

    @Test
    @DisplayName("GET /songs/{id} should return 404 when not found")
    void getSongs_shouldReturn404WhenNotFound() throws Exception {
        mockMvc.perform(get("/songs/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("404"))
                .andExpect(jsonPath("$.errorMessage").value("Resource with ID=999 not found"));
    }

    @Test
    @DisplayName("DELETE /songs should delete only existing resources and return their IDs")
    void deleteSongs_shouldDeleteExistingAndReturnIds() throws Exception {
        songRepository.saveAllAndFlush(List.of(
                SongEntity.builder()
                        .resourceId(1L)
                        .name("A")
                        .artist("A")
                        .album("A")
                        .duration(Duration.ofSeconds(30))
                        .year(LocalDate.of(2020, 1, 1))
                        .build(),
                SongEntity.builder()
                        .resourceId(3L)
                        .name("B")
                        .artist("B")
                        .album("B")
                        .duration(Duration.ofSeconds(31))
                        .year(LocalDate.of(2021, 1, 1))
                        .build()
        ));

        MvcResult result = mockMvc.perform(delete("/songs")
                        .param("id", "1,999,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ids").isArray())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        List<Long> deletedIds = new ArrayList<>();
        json.get("ids").forEach(n -> deletedIds.add(n.asLong()));

        assertEquals(2, deletedIds.size());
        assertTrue(deletedIds.containsAll(List.of(1L, 3L)));

        assertTrue(songRepository.findByResourceId(1L).isEmpty());
        assertTrue(songRepository.findByResourceId(3L).isEmpty());
    }

    @Test
    @DisplayName("DELETE /songs should return 400 for invalid id format")
    void deleteSongs_shouldReturn400ForInvalidFormat() throws Exception {
        mockMvc.perform(delete("/songs")
                        .param("id", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("400"))
                .andExpect(jsonPath("$.errorMessage").value("Id must be comma-separated numbers or single number"));
    }
}
