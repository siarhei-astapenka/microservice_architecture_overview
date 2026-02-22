package com.epam.learn.song_service.service.mapper;

import com.epam.learn.song_service.entity.SongEntity;
import com.epam.learn.song_service.model.SongMetadataRequest;
import com.epam.learn.song_service.model.SongMetadataResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SongMapper Unit Tests")
class SongMapperTest {

    private SongMapper songMapper;

    @BeforeEach
    void setUp() {
        songMapper = new SongMapper();
    }

    @Nested
    @DisplayName("toEntity")
    class ToEntityTests {

        @Test
        @DisplayName("Should convert request to entity correctly")
        void toEntity_shouldConvertRequestToEntity() {
            // Given
            SongMetadataRequest request = SongMetadataRequest.builder()
                    .id(100L)
                    .name("Test Song")
                    .artist("Test Artist")
                    .album("Test Album")
                    .duration("03:30")
                    .year("2024")
                    .build();

            // When
            SongEntity result = songMapper.toEntity(request);

            // Then
            assertNotNull(result);
            assertEquals(100L, result.getResourceId());
            assertEquals("Test Song", result.getName());
            assertEquals("Test Artist", result.getArtist());
            assertEquals("Test Album", result.getAlbum());
            assertEquals(Duration.ofMinutes(3).plusSeconds(30), result.getDuration());
            assertEquals(LocalDate.of(2024, 1, 1), result.getYear());
        }

        @Test
        @DisplayName("Should handle zero duration")
        void toEntity_shouldHandleZeroDuration() {
            // Given
            SongMetadataRequest request = SongMetadataRequest.builder()
                    .id(1L)
                    .name("Song")
                    .artist("Artist")
                    .album("Album")
                    .duration("00:00")
                    .year("2020")
                    .build();

            // When
            SongEntity result = songMapper.toEntity(request);

            // Then
            assertEquals(Duration.ZERO, result.getDuration());
        }

        @Test
        @DisplayName("Should handle maximum duration (59:59)")
        void toEntity_shouldHandleMaxDuration() {
            // Given
            SongMetadataRequest request = SongMetadataRequest.builder()
                    .id(1L)
                    .name("Song")
                    .artist("Artist")
                    .album("Album")
                    .duration("59:59")
                    .year("2020")
                    .build();

            // When
            SongEntity result = songMapper.toEntity(request);

            // Then
            assertEquals(Duration.ofMinutes(59).plusSeconds(59), result.getDuration());
        }

        @Test
        @DisplayName("Should handle different years")
        void toEntity_shouldHandleDifferentYears() {
            // Given
            SongMetadataRequest request = SongMetadataRequest.builder()
                    .id(1L)
                    .name("Song")
                    .artist("Artist")
                    .album("Album")
                    .duration("03:00")
                    .year("1999")
                    .build();

            // When
            SongEntity result = songMapper.toEntity(request);

            // Then
            assertEquals(LocalDate.of(1999, 1, 1), result.getYear());
        }
    }

    @Nested
    @DisplayName("toResponse")
    class ToResponseTests {

        @Test
        @DisplayName("Should convert entity to response correctly")
        void toResponse_shouldConvertEntityToResponse() {
            // Given
            SongEntity entity = SongEntity.builder()
                    .id(1L)
                    .resourceId(100L)
                    .name("Test Song")
                    .artist("Test Artist")
                    .album("Test Album")
                    .duration(Duration.ofMinutes(3).plusSeconds(30))
                    .year(LocalDate.of(2024, 1, 1))
                    .build();

            // When
            SongMetadataResponse result = songMapper.toResponse(entity);

            // Then
            assertNotNull(result);
            assertEquals(100L, result.getId());
            assertEquals("Test Song", result.getName());
            assertEquals("Test Artist", result.getArtist());
            assertEquals("Test Album", result.getAlbum());
            assertEquals("03:30", result.getDuration());
            assertEquals("2024", result.getYear());
        }

        @Test
        @DisplayName("Should handle zero duration in response")
        void toResponse_shouldHandleZeroDuration() {
            // Given
            SongEntity entity = SongEntity.builder()
                    .id(1L)
                    .resourceId(1L)
                    .name("Song")
                    .artist("Artist")
                    .album("Album")
                    .duration(Duration.ZERO)
                    .year(LocalDate.of(2020, 1, 1))
                    .build();

            // When
            SongMetadataResponse result = songMapper.toResponse(entity);

            // Then
            assertEquals("00:00", result.getDuration());
        }

        @Test
        @DisplayName("Should handle maximum duration in response")
        void toResponse_shouldHandleMaxDuration() {
            // Given
            SongEntity entity = SongEntity.builder()
                    .id(1L)
                    .resourceId(1L)
                    .name("Song")
                    .artist("Artist")
                    .album("Album")
                    .duration(Duration.ofMinutes(59).plusSeconds(59))
                    .year(LocalDate.of(2020, 1, 1))
                    .build();

            // When
            SongMetadataResponse result = songMapper.toResponse(entity);

            // Then
            assertEquals("59:59", result.getDuration());
        }

        @Test
        @DisplayName("Should format year correctly in response")
        void toResponse_shouldFormatYearCorrectly() {
            // Given
            SongEntity entity = SongEntity.builder()
                    .id(1L)
                    .resourceId(1L)
                    .name("Song")
                    .artist("Artist")
                    .album("Album")
                    .duration(Duration.ofMinutes(3))
                    .year(LocalDate.of(1999, 6, 15))
                    .build();

            // When
            SongMetadataResponse result = songMapper.toResponse(entity);

            // Then
            assertEquals("1999", result.getYear());
        }
    }

    @Nested
    @DisplayName("Duration parsing and formatting")
    class DurationTests {

        @Test
        @DisplayName("Should parse various duration formats")
        void parseDuration_variousFormats() {
            // Test through toEntity
            SongMetadataRequest request1 = createRequestWithDuration("01:30");
            SongEntity entity1 = songMapper.toEntity(request1);
            assertEquals(Duration.ofMinutes(1).plusSeconds(30), entity1.getDuration());

            SongMetadataRequest request2 = createRequestWithDuration("10:05");
            SongEntity entity2 = songMapper.toEntity(request2);
            assertEquals(Duration.ofMinutes(10).plusSeconds(5), entity2.getDuration());

            SongMetadataRequest request3 = createRequestWithDuration("00:45");
            SongEntity entity3 = songMapper.toEntity(request3);
            assertEquals(Duration.ofMinutes(0).plusSeconds(45), entity3.getDuration());
        }

        @Test
        @DisplayName("Should format various durations correctly")
        void formatDuration_variousDurations() {
            // Test through toResponse
            SongEntity entity1 = createEntityWithDuration(Duration.ofMinutes(5).plusSeconds(15));
            SongMetadataResponse response1 = songMapper.toResponse(entity1);
            assertEquals("05:15", response1.getDuration());

            SongEntity entity2 = createEntityWithDuration(Duration.ofMinutes(12).plusSeconds(0));
            SongMetadataResponse response2 = songMapper.toResponse(entity2);
            assertEquals("12:00", response2.getDuration());

            SongEntity entity3 = createEntityWithDuration(Duration.ofMinutes(0).plusSeconds(5));
            SongMetadataResponse response3 = songMapper.toResponse(entity3);
            assertEquals("00:05", response3.getDuration());
        }

        private SongMetadataRequest createRequestWithDuration(String duration) {
            return SongMetadataRequest.builder()
                    .id(1L)
                    .name("Test")
                    .artist("Test")
                    .album("Test")
                    .duration(duration)
                    .year("2024")
                    .build();
        }

        private SongEntity createEntityWithDuration(Duration duration) {
            return SongEntity.builder()
                    .id(1L)
                    .resourceId(1L)
                    .name("Test")
                    .artist("Test")
                    .album("Test")
                    .duration(duration)
                    .year(LocalDate.of(2024, 1, 1))
                    .build();
        }
    }

    @Nested
    @DisplayName("Year parsing and formatting")
    class YearTests {

        @Test
        @DisplayName("Should parse various year formats")
        void parseYear_variousFormats() {
            // Test through toEntity
            SongMetadataRequest request1 = createRequestWithYear("2000");
            SongEntity entity1 = songMapper.toEntity(request1);
            assertEquals(LocalDate.of(2000, 1, 1), entity1.getYear());

            SongMetadataRequest request2 = createRequestWithYear("2025");
            SongEntity entity2 = songMapper.toEntity(request2);
            assertEquals(LocalDate.of(2025, 1, 1), entity2.getYear());

            SongMetadataRequest request3 = createRequestWithYear("1985");
            SongEntity entity3 = songMapper.toEntity(request3);
            assertEquals(LocalDate.of(1985, 1, 1), entity3.getYear());
        }

        @Test
        @DisplayName("Should format years correctly")
        void formatYear_variousYears() {
            // Test through toResponse
            SongEntity entity1 = createEntityWithYear(LocalDate.of(2000, 5, 15));
            SongMetadataResponse response1 = songMapper.toResponse(entity1);
            assertEquals("2000", response1.getYear());

            SongEntity entity2 = createEntityWithYear(LocalDate.of(2025, 12, 31));
            SongMetadataResponse response2 = songMapper.toResponse(entity2);
            assertEquals("2025", response2.getYear());
        }

        private SongMetadataRequest createRequestWithYear(String year) {
            return SongMetadataRequest.builder()
                    .id(1L)
                    .name("Test")
                    .artist("Test")
                    .album("Test")
                    .duration("03:00")
                    .year(year)
                    .build();
        }

        private SongEntity createEntityWithYear(LocalDate year) {
            return SongEntity.builder()
                    .id(1L)
                    .resourceId(1L)
                    .name("Test")
                    .artist("Test")
                    .album("Test")
                    .duration(Duration.ofMinutes(3))
                    .year(year)
                    .build();
        }
    }
}
