package com.epam.learn.song_service.model.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.Duration;

public class SongDurationSerializer extends JsonSerializer<Duration> {
    @Override
    public void serialize(Duration duration, JsonGenerator gen,
            SerializerProvider provider) throws IOException {
        if (duration == null) {
            gen.writeNull();
            return;
        }

        long totalSeconds = duration.getSeconds();
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        gen.writeString(String.format("%02d:%02d", minutes, seconds));
    }
}
