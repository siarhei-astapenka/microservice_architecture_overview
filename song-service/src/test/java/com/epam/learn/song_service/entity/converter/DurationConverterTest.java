package com.epam.learn.song_service.entity.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DurationConverter Unit Tests")
class DurationConverterTest {

    private DurationConverter durationConverter;

    @BeforeEach
    void setUp() {
        durationConverter = new DurationConverter();
    }

    @Test
    @DisplayName("Should convert Duration to database column (seconds)")
    void convertToDatabaseColumn_shouldConvertDurationToSeconds() {
        // Given
        Duration duration = Duration.ofMinutes(3).plusSeconds(30); // 3:30 = 210 seconds

        // When
        Long result = durationConverter.convertToDatabaseColumn(duration);

        // Then
        assertEquals(210L, result);
    }

    @Test
    @DisplayName("Should return null when Duration is null")
    void convertToDatabaseColumn_shouldReturnNullForNullDuration() {
        // Given
        Duration duration = null;

        // When
        Long result = durationConverter.convertToDatabaseColumn(duration);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should convert seconds to Duration")
    void convertToEntityAttribute_shouldConvertSecondsToDuration() {
        // Given
        Long seconds = 210L; // 3 minutes 30 seconds

        // When
        Duration result = durationConverter.convertToEntityAttribute(seconds);

        // Then
        assertNotNull(result);
        assertEquals(3, result.toMinutes());
        assertEquals(30, result.getSeconds() % 60);
    }

    @Test
    @DisplayName("Should return null when seconds is null")
    void convertToEntityAttribute_shouldReturnNullForNullSeconds() {
        // Given
        Long seconds = null;

        // When
        Duration result = durationConverter.convertToEntityAttribute(seconds);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should handle zero seconds")
    void convertToDatabaseColumn_shouldHandleZeroSeconds() {
        // Given
        Duration duration = Duration.ZERO;

        // When
        Long result = durationConverter.convertToDatabaseColumn(duration);

        // Then
        assertEquals(0L, result);
    }

    @Test
    @DisplayName("Should handle large duration values")
    void convertToDatabaseColumn_shouldHandleLargeDuration() {
        // Given
        Duration duration = Duration.ofHours(1).plusMinutes(30).plusSeconds(45); // 1:30:45 = 5445 seconds

        // When
        Long result = durationConverter.convertToDatabaseColumn(duration);

        // Then
        assertEquals(5445L, result);
    }

    @Test
    @DisplayName("Should handle large seconds values for entity attribute")
    void convertToEntityAttribute_shouldHandleLargeSeconds() {
        // Given
        Long seconds = 5445L; // 1 hour 30 minutes 45 seconds

        // When
        Duration result = durationConverter.convertToEntityAttribute(seconds);

        // Then
        assertNotNull(result);
        assertEquals(1, result.toHours());
        assertEquals(30, result.toMinutes() % 60);
        assertEquals(45, result.getSeconds() % 60);
    }
}
