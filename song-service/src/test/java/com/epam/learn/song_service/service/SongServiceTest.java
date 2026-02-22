package com.epam.learn.song_service.service;

import com.epam.learn.song_service.entity.SongEntity;
import com.epam.learn.song_service.exception.ConflictException;
import com.epam.learn.song_service.exception.NotFoundException;
import com.epam.learn.song_service.model.SongMetadataRequest;
import com.epam.learn.song_service.model.SongMetadataResponse;
import com.epam.learn.song_service.repository.SongRepository;
import com.epam.learn.song_service.service.mapper.SongMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SongService Unit Tests")
class SongServiceTest {

    @Mock
    private SongRepository songRepository;

    @Mock
    private SongMapper songMapper;

    @InjectMocks
    private SongService songService;

    @Nested
    @DisplayName("saveSongMetadata")
    class SaveSongMetadataTests {

        @Test
        @DisplayName("Should save song metadata successfully")
        void saveSongMetadata_shouldSaveSuccessfully() {
            // Given
            SongMetadataRequest request = createRequest();
            SongEntity entity = createEntity();
            SongEntity savedEntity = createEntity();
            savedEntity.setId(1L);

            when(songMapper.toEntity(request)).thenReturn(entity);
            when(songRepository.findByResourceId(100L)).thenReturn(Optional.empty());
            when(songRepository.save(any(SongEntity.class))).thenReturn(savedEntity);

            // When
            SongMetadataResponse result = songService.saveSongMetadata(request);

            // Then
            assertNotNull(result);
            assertEquals(1L, result.getId());
            verify(songRepository).save(any(SongEntity.class));
        }

        @Test
        @DisplayName("Should throw ConflictException when resource ID already exists")
        void saveSongMetadata_shouldThrowConflictException() {
            // Given
            SongMetadataRequest request = createRequest();
            SongEntity existingEntity = createEntity();

            when(songMapper.toEntity(request)).thenReturn(existingEntity);
            when(songRepository.findByResourceId(100L)).thenReturn(Optional.of(existingEntity));

            // When & Then
            ConflictException exception = assertThrows(ConflictException.class,
                    () -> songService.saveSongMetadata(request));

            assertTrue(exception.getMessage().contains("already exists"));
            verify(songRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getSongMetadataByResourceId")
    class GetSongMetadataByResourceIdTests {

        @Test
        @DisplayName("Should return song metadata when found")
        void getSongMetadataByResourceId_shouldReturnWhenFound() {
            // Given
            Long resourceId = 100L;
            SongEntity entity = createEntity();
            SongMetadataResponse expectedResponse = SongMetadataResponse.builder()
                    .id(1L)
                    .name("Test Song")
                    .artist("Test Artist")
                    .album("Test Album")
                    .duration("03:00")
                    .year("2024")
                    .build();

            when(songRepository.findByResourceId(resourceId)).thenReturn(Optional.of(entity));
            when(songMapper.toResponse(entity)).thenReturn(expectedResponse);

            // When
            SongMetadataResponse result = songService.getSongMetadataByResourceId(resourceId);

            // Then
            assertNotNull(result);
            assertEquals(expectedResponse.getName(), result.getName());
            verify(songRepository).findByResourceId(resourceId);
        }

        @Test
        @DisplayName("Should throw NotFoundException when not found")
        void getSongMetadataByResourceId_shouldThrowNotFoundException() {
            // Given
            Long resourceId = 999L;
            when(songRepository.findByResourceId(resourceId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> songService.getSongMetadataByResourceId(resourceId));
        }
    }

    @Nested
    @DisplayName("deleteSongMetadata")
    class DeleteSongMetadataTests {

        @Test
        @DisplayName("Should delete song metadata successfully")
        void deleteSongMetadata_shouldDeleteSuccessfully() {
            // Given
            String ids = "1,2,3";
            List<Long> existingIds = List.of(1L, 2L, 3L);

            when(songRepository.findExistingIds(anyList())).thenReturn(existingIds);
            doNothing().when(songRepository).deleteAllByResourceIdIn(existingIds);

            // When
            var result = songService.deleteSongMetadata(ids);

            // Then
            assertNotNull(result);
            assertEquals(3, result.get("ids").size());
            verify(songRepository).deleteAllByResourceIdIn(existingIds);
        }

        @Test
        @DisplayName("Should return empty list when no songs found")
        void deleteSongMetadata_shouldReturnEmptyList() {
            // Given
            String ids = "999,998";
            when(songRepository.findExistingIds(anyList())).thenReturn(Collections.emptyList());

            // When
            var result = songService.deleteSongMetadata(ids);

            // Then
            assertNotNull(result);
            assertTrue(result.get("ids").isEmpty());
            // Note: deleteAllByResourceIdIn is ALWAYS called by the service, even with empty list
            verify(songRepository).deleteAllByResourceIdIn(Collections.emptyList());
        }

        @Test
        @DisplayName("Should handle single id")
        void deleteSongMetadata_shouldHandleSingleId() {
            // Given
            String ids = "42";
            List<Long> existingIds = List.of(42L);

            when(songRepository.findExistingIds(anyList())).thenReturn(existingIds);
            doNothing().when(songRepository).deleteAllByResourceIdIn(existingIds);

            // When
            var result = songService.deleteSongMetadata(ids);

            // Then
            assertNotNull(result);
            assertEquals(1, result.get("ids").size());
            assertTrue(result.get("ids").contains(42L));
        }

        @Test
        @DisplayName("Should delete only existing resources")
        void deleteSongMetadata_shouldDeleteOnlyExisting() {
            // Given
            String ids = "1,999,3"; // 999 doesn't exist
            List<Long> existingIds = List.of(1L, 3L);

            when(songRepository.findExistingIds(anyList())).thenReturn(existingIds);
            doNothing().when(songRepository).deleteAllByResourceIdIn(existingIds);

            // When
            var result = songService.deleteSongMetadata(ids);

            // Then
            assertNotNull(result);
            assertEquals(2, result.get("ids").size());
            assertFalse(result.get("ids").contains(999L));
        }
    }

    private SongMetadataRequest createRequest() {
        return SongMetadataRequest.builder()
                .id(100L)
                .name("Test Song")
                .artist("Test Artist")
                .album("Test Album")
                .duration("03:00")
                .year("2024")
                .build();
    }

    private SongEntity createEntity() {
        return SongEntity.builder()
                .id(1L)
                .resourceId(100L)
                .name("Test Song")
                .artist("Test Artist")
                .album("Test Album")
                .duration(Duration.ofMinutes(3))
                .year(LocalDate.of(2024, 1, 1))
                .build();
    }
}
