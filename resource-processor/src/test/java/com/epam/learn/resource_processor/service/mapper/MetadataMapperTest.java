package com.epam.learn.resource_processor.service.mapper;

import com.epam.learn.resource_processor.dto.SongMetadataRequest;
import com.epam.learn.resource_processor.model.metadata.Metadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MetadataMapper Unit Tests")
class MetadataMapperTest {

    private MetadataMapper metadataMapper;

    @BeforeEach
    void setUp() {
        metadataMapper = new MetadataMapper();
    }

    @Nested
    @DisplayName("toSongMetadataRequest")
    class ToSongMetadataRequestTests {

        @Test
        @DisplayName("Should map metadata to request correctly")
        void shouldMapCorrectly() {
            // Given
            Metadata metadata = Metadata.builder()
                    .name("Test Song")
                    .artist("Test Artist")
                    .album("Test Album")
                    .duration(Duration.ofMinutes(3).plusSeconds(30))
                    .year(LocalDate.of(2024, 5, 15))
                    .build();
            Long resourceId = 1L;

            // When
            SongMetadataRequest result = metadataMapper.toSongMetadataRequest(metadata, resourceId);

            // Then
            assertNotNull(result);
            assertEquals(resourceId, result.getId());
            assertEquals("Test Song", result.getName());
            assertEquals("Test Artist", result.getArtist());
            assertEquals("Test Album", result.getAlbum());
            assertEquals("03:30", result.getDuration());
            assertEquals("2024", result.getYear());
        }

        @Test
        @DisplayName("Should format duration correctly for zero minutes")
        void shouldFormatZeroMinutes() {
            // Given
            Metadata metadata = Metadata.builder()
                    .name("Short Song")
                    .artist("Artist")
                    .album("Album")
                    .duration(Duration.ofSeconds(45))
                    .year(LocalDate.of(2024, 1, 1))
                    .build();

            // When
            SongMetadataRequest result = metadataMapper.toSongMetadataRequest(metadata, 1L);

            // Then
            assertEquals("00:45", result.getDuration());
        }

        @Test
        @DisplayName("Should format duration correctly for full minutes")
        void shouldFormatFullMinutes() {
            // Given
            Metadata metadata = Metadata.builder()
                    .name("Long Song")
                    .artist("Artist")
                    .album("Album")
                    .duration(Duration.ofMinutes(10).plusSeconds(5))
                    .year(LocalDate.of(2024, 1, 1))
                    .build();

            // When
            SongMetadataRequest result = metadataMapper.toSongMetadataRequest(metadata, 1L);

            // Then
            assertEquals("10:05", result.getDuration());
        }

        @Test
        @DisplayName("Should format year correctly")
        void shouldFormatYearCorrectly() {
            // Given
            Metadata metadata = Metadata.builder()
                    .name("Song")
                    .artist("Artist")
                    .album("Album")
                    .duration(Duration.ofMinutes(3))
                    .year(LocalDate.of(1999, 12, 31))
                    .build();

            // When
            SongMetadataRequest result = metadataMapper.toSongMetadataRequest(metadata, 1L);

            // Then
            assertEquals("1999", result.getYear());
        }

        @Test
        @DisplayName("Should handle null duration gracefully")
        void shouldHandleNullDuration() {
            // Given
            Metadata metadata = Metadata.builder()
                    .name("Song")
                    .artist("Artist")
                    .album("Album")
                    .duration(null)
                    .year(LocalDate.of(2024, 1, 1))
                    .build();

            // When & Then
            assertThrows(NullPointerException.class, () -> 
                metadataMapper.toSongMetadataRequest(metadata, 1L));
        }

        @Test
        @DisplayName("Should handle null year gracefully")
        void shouldHandleNullYear() {
            // Given
            Metadata metadata = Metadata.builder()
                    .name("Song")
                    .artist("Artist")
                    .album("Album")
                    .duration(Duration.ofMinutes(3))
                    .year(null)
                    .build();

            // When & Then
            assertThrows(NullPointerException.class, () -> 
                metadataMapper.toSongMetadataRequest(metadata, 1L));
        }

        @Test
        @DisplayName("Should handle null metadata")
        void shouldHandleNullMetadata() {
            // When & Then
            assertThrows(NullPointerException.class, () -> 
                metadataMapper.toSongMetadataRequest(null, 1L));
        }

        @Test
        @DisplayName("Should handle null resourceId")
        void shouldHandleNullResourceId() {
            // Given
            Metadata metadata = Metadata.builder()
                    .name("Song")
                    .artist("Artist")
                    .album("Album")
                    .duration(Duration.ofMinutes(3))
                    .year(LocalDate.of(2024, 1, 1))
                    .build();

            // When
            SongMetadataRequest result = metadataMapper.toSongMetadataRequest(metadata, null);

            // Then
            assertNotNull(result);
            assertNull(result.getId());
        }

        @Test
        @DisplayName("Should format long duration correctly")
        void shouldFormatLongDuration() {
            // Given - 59 minutes 59 seconds (max for toMinutesPart)
            Metadata metadata = Metadata.builder()
                    .name("Long Song")
                    .artist("Artist")
                    .album("Album")
                    .duration(Duration.ofMinutes(59).plusSeconds(59))
                    .year(LocalDate.of(2024, 1, 1))
                    .build();

            // When
            SongMetadataRequest result = metadataMapper.toSongMetadataRequest(metadata, 1L);

            // Then
            assertEquals("59:59", result.getDuration());
        }

        @Test
        @DisplayName("Should handle null name")
        void shouldHandleNullName() {
            // Given
            Metadata metadata = Metadata.builder()
                    .name(null)
                    .artist("Artist")
                    .album("Album")
                    .duration(Duration.ofMinutes(3))
                    .year(LocalDate.of(2024, 1, 1))
                    .build();

            // When
            SongMetadataRequest result = metadataMapper.toSongMetadataRequest(metadata, 1L);

            // Then
            assertNotNull(result);
            assertNull(result.getName());
        }

        @Test
        @DisplayName("Should handle null artist")
        void shouldHandleNullArtist() {
            // Given
            Metadata metadata = Metadata.builder()
                    .name("Song")
                    .artist(null)
                    .album("Album")
                    .duration(Duration.ofMinutes(3))
                    .year(LocalDate.of(2024, 1, 1))
                    .build();

            // When
            SongMetadataRequest result = metadataMapper.toSongMetadataRequest(metadata, 1L);

            // Then
            assertNotNull(result);
            assertNull(result.getArtist());
        }

        @Test
        @DisplayName("Should handle null album")
        void shouldHandleNullAlbum() {
            // Given
            Metadata metadata = Metadata.builder()
                    .name("Song")
                    .artist("Artist")
                    .album(null)
                    .duration(Duration.ofMinutes(3))
                    .year(LocalDate.of(2024, 1, 1))
                    .build();

            // When
            SongMetadataRequest result = metadataMapper.toSongMetadataRequest(metadata, 1L);

            // Then
            assertNotNull(result);
            assertNull(result.getAlbum());
        }

        @Test
        @DisplayName("Should format year from early date")
        void shouldFormatEarlyYear() {
            // Given
            Metadata metadata = Metadata.builder()
                    .name("Old Song")
                    .artist("Artist")
                    .album("Album")
                    .duration(Duration.ofMinutes(3))
                    .year(LocalDate.of(1950, 6, 15))
                    .build();

            // When
            SongMetadataRequest result = metadataMapper.toSongMetadataRequest(metadata, 1L);

            // Then
            assertEquals("1950", result.getYear());
        }

        @Test
        @DisplayName("Should handle zero duration")
        void shouldHandleZeroDuration() {
            // Given
            Metadata metadata = Metadata.builder()
                    .name("Empty Song")
                    .artist("Artist")
                    .album("Album")
                    .duration(Duration.ZERO)
                    .year(LocalDate.of(2024, 1, 1))
                    .build();

            // When
            SongMetadataRequest result = metadataMapper.toSongMetadataRequest(metadata, 1L);

            // Then
            assertEquals("00:00", result.getDuration());
        }
    }
}
