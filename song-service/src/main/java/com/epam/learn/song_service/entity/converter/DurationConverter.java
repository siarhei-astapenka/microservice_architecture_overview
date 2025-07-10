package com.epam.learn.song_service.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Duration;

@Converter
public class DurationConverter implements AttributeConverter<Duration, Long> {
    @Override
    public Long convertToDatabaseColumn(Duration duration) {
        return duration == null ? null : duration.getSeconds();
    }

    @Override
    public Duration convertToEntityAttribute(Long seconds) {
        return seconds == null ? null : Duration.ofSeconds(seconds);
    }
}
