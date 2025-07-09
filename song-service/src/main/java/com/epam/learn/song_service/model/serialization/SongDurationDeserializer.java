package com.epam.learn.song_service.model.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class SongDurationDeserializer extends JsonDeserializer<Duration> {
    @Override
    public Duration deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {
        String value = p.getText().trim();

        if (!value.matches("^\\d{2}:\\d{2}$")) {
            throw new JsonMappingException(p,
                    "Duration must be in strict mm:ss format with leading zeros (e.g., '03:45')");
        }

        String[] parts = value.split(":");
        try {
            long minutes = TimeUnit.MINUTES.toSeconds(Long.parseLong(parts[0]));
            long seconds = Long.parseLong(parts[1]);
            return Duration.ofSeconds(minutes + seconds);
        } catch (NumberFormatException e) {
            throw new JsonMappingException(p, "Invalid number in duration");
        }
    }
}
