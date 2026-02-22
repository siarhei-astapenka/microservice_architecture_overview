package com.epam.learn.resource_processor.service.parser;

import com.epam.learn.resource_processor.exception.BadRequestException;
import com.epam.learn.resource_processor.model.metadata.Metadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MetadataParser Unit Tests")
class MetadataParserTest {

    private MetadataParser metadataParser;

    @BeforeEach
    void setUp() {
        metadataParser = new MetadataParser();
    }

    @Nested
    @DisplayName("getSongMetadataFromFile")
    class GetSongMetadataFromFileTests {

        @Test
        @DisplayName("Should return empty for null file")
        void shouldReturnEmptyForNullFile() {
            // When
            Optional<Metadata> result = metadataParser.getSongMetadataFromFile(null);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return empty for empty byte array")
        void shouldReturnEmptyForEmptyArray() {
            // When
            Optional<Metadata> result = metadataParser.getSongMetadataFromFile(new byte[0]);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return metadata for non-MP3 data (Tika parses anyway)")
        void shouldReturnMetadataForNonMp3Data() {
            // Given - Tika's Mp3Parser doesn't throw exceptions for invalid data
            // It just returns empty metadata
            byte[] invalidData = "This is not an MP3 file".getBytes();

            // When
            Optional<Metadata> result = metadataParser.getSongMetadataFromFile(invalidData);

            // Then - Tika parses without error, returns metadata with null fields
            assertTrue(result.isPresent());
        }

        @Test
        @DisplayName("Should return metadata for random bytes")
        void shouldReturnMetadataForRandomBytes() {
            // Given - Tika's Mp3Parser handles random bytes gracefully
            byte[] randomBytes = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05};

            // When
            Optional<Metadata> result = metadataParser.getSongMetadataFromFile(randomBytes);

            // Then
            assertTrue(result.isPresent());
        }
    }

    @Nested
    @DisplayName("parseDuration")
    class ParseDurationTests {

        @Test
        @DisplayName("Should parse valid duration string")
        void shouldParseValidDuration() {
            // Given
            String durationString = "180.5"; // 180.5 seconds

            // When
            Duration result = ReflectionTestUtils.invokeMethod(
                    metadataParser, "parseDuration", durationString);

            // Then
            assertNotNull(result);
            assertEquals(180500, result.toMillis()); // 180.5 seconds in millis
        }

        @Test
        @DisplayName("Should return null for null duration")
        void shouldReturnNullForNullDuration() {
            // When
            Duration result = ReflectionTestUtils.invokeMethod(
                    metadataParser, "parseDuration", (String) null);

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("Should return null for empty duration")
        void shouldReturnNullForEmptyDuration() {
            // When
            Duration result = ReflectionTestUtils.invokeMethod(
                    metadataParser, "parseDuration", "");

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("Should return null for whitespace duration")
        void shouldReturnNullForWhitespaceDuration() {
            // When
            Duration result = ReflectionTestUtils.invokeMethod(
                    metadataParser, "parseDuration", "   ");

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("Should throw BadRequestException for invalid duration format")
        void shouldThrowForInvalidDuration() {
            // When & Then
            assertThrows(BadRequestException.class, () -> 
                ReflectionTestUtils.invokeMethod(metadataParser, "parseDuration", "not-a-number")
            );
        }

        @Test
        @DisplayName("Should parse duration with decimal point")
        void shouldParseDecimalDuration() {
            // Given
            String durationString = "3.14159";

            // When
            Duration result = ReflectionTestUtils.invokeMethod(
                    metadataParser, "parseDuration", durationString);

            // Then
            assertNotNull(result);
            assertEquals(3141, result.toMillis()); // ~3.14159 seconds
        }
    }

    @Nested
    @DisplayName("parseReleaseDate")
    class ParseReleaseDateTests {

        @Test
        @DisplayName("Should parse valid ISO date")
        void shouldParseValidIsoDate() {
            // Given
            String dateString = "2024-05-15";

            // When
            LocalDate result = ReflectionTestUtils.invokeMethod(
                    metadataParser, "parseReleaseDate", dateString);

            // Then
            assertNotNull(result);
            assertEquals(2024, result.getYear());
            assertEquals(5, result.getMonthValue());
            assertEquals(15, result.getDayOfMonth());
        }

        @Test
        @DisplayName("Should parse year-only string")
        void shouldParseYearOnly() {
            // Given
            String dateString = "2024";

            // When
            LocalDate result = ReflectionTestUtils.invokeMethod(
                    metadataParser, "parseReleaseDate", dateString);

            // Then
            assertNotNull(result);
            assertEquals(2024, result.getYear());
            assertEquals(1, result.getDayOfYear());
        }

        @Test
        @DisplayName("Should return null for null date")
        void shouldReturnNullForNullDate() {
            // When
            LocalDate result = ReflectionTestUtils.invokeMethod(
                    metadataParser, "parseReleaseDate", (String) null);

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("Should return null for empty date")
        void shouldReturnNullForEmptyDate() {
            // When
            LocalDate result = ReflectionTestUtils.invokeMethod(
                    metadataParser, "parseReleaseDate", "");

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("Should throw BadRequestException for invalid date format")
        void shouldThrowForInvalidDate() {
            // When & Then
            assertThrows(BadRequestException.class, () -> 
                ReflectionTestUtils.invokeMethod(metadataParser, "parseReleaseDate", "invalid-date")
            );
        }

        @Test
        @DisplayName("Should parse date with whitespace")
        void shouldParseDateWithWhitespace() {
            // Given
            String dateString = "  2024-05-15  ";

            // When
            LocalDate result = ReflectionTestUtils.invokeMethod(
                    metadataParser, "parseReleaseDate", dateString);

            // Then
            assertNotNull(result);
            assertEquals(2024, result.getYear());
        }
    }

    @Nested
    @DisplayName("getSafeMetadataValue")
    class GetSafeMetadataValueTests {

        @Test
        @DisplayName("Should return trimmed value")
        void shouldReturnTrimmedValue() {
            // Given
            org.apache.tika.metadata.Metadata metadata = new org.apache.tika.metadata.Metadata();
            metadata.set("title", "  Test Song  ");

            // When
            String result = ReflectionTestUtils.invokeMethod(
                    metadataParser, "getSafeMetadataValue", metadata, "title");

            // Then
            assertEquals("Test Song", result);
        }

        @Test
        @DisplayName("Should return null for missing key")
        void shouldReturnNullForMissingKey() {
            // Given
            org.apache.tika.metadata.Metadata metadata = new org.apache.tika.metadata.Metadata();

            // When
            String result = ReflectionTestUtils.invokeMethod(
                    metadataParser, "getSafeMetadataValue", metadata, "nonexistent");

            // Then
            assertNull(result);
        }
    }
}
