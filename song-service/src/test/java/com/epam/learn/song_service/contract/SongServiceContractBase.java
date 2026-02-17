package com.epam.learn.song_service.contract;

import com.epam.learn.song_service.controller.SongServiceController;
import com.epam.learn.song_service.model.SongMetadataRequest;
import com.epam.learn.song_service.model.SongMetadataResponse;
import com.epam.learn.song_service.service.SongService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Base class for Spring Cloud Contract producer tests in song-service.
 * Provides MockMvc setup and mock stubs for the SongService.
 */
@WebMvcTest(SongServiceController.class)
@ActiveProfiles("test")
public abstract class SongServiceContractBase {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SongService songService;

    @BeforeEach
    public void setup() {
        RestAssuredMockMvc.mockMvc(mockMvc);

        // Stub for POST /songs - save song metadata
        SongMetadataResponse savedResponse = SongMetadataResponse.builder()
                .id(1L)
                .build();
        when(songService.saveSongMetadata(any(SongMetadataRequest.class))).thenReturn(savedResponse);

        // Stub for GET /songs/{id} - get song metadata by resource id
        SongMetadataResponse getResponse = SongMetadataResponse.builder()
                .id(1L)
                .name("Test Song")
                .artist("Test Artist")
                .album("Test Album")
                .duration("03:45")
                .year("2023")
                .build();
        when(songService.getSongMetadataByResourceId(anyLong())).thenReturn(getResponse);

        // Stub for DELETE /songs - delete song metadata
        when(songService.deleteSongMetadata(anyString())).thenReturn(Map.of("ids", List.of(1L)));
    }
}
