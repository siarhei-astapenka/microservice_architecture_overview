package com.epam.learn.song_service.model;

import com.epam.learn.song_service.model.serialization.SongDuration;
import com.epam.learn.song_service.validation.constraints.ValidSongDuration;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.Year;

@Value
@Jacksonized
@Builder
@JsonPropertyOrder({
        "id", "name", "artist", "album", "duration", "year"
})
@Validated
public class SongRequest {
    @NotNull @Positive Long id;
    @NotBlank @Size(min = 1, max = 100) String name;
    @NotBlank @Size(min = 1, max = 100) String artist;
    @NotBlank @Size(min = 1, max = 100) String album;
    @NotNull @SongDuration @Valid @ValidSongDuration Duration duration;
    @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy") Year year;
}
