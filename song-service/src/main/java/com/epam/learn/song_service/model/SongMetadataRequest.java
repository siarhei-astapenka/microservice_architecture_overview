package com.epam.learn.song_service.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.springframework.validation.annotation.Validated;

@Value
@Jacksonized
@Builder
@JsonPropertyOrder({
        "id", "name", "artist", "album", "duration", "year"
})
@Validated
public class SongMetadataRequest {
    @NotNull(message = "ID is required") @Min(value = 1, message = "ID must be greater than or equal to {value}")
    Long id;

    @NotBlank(message = "Song name is required") @Size(min = 1, max = 100, message = "Song name size must be between {min} and {max}")
    String name;

    @NotBlank(message = "Artist name is required") @Size(min = 1, max = 100, message = "Artist name size must be between {min} and {max}")
    String artist;

    @NotBlank(message = "Album name is required") @Size(min = 1, max = 100, message = "Album name size must be between {min} and {max}")
    String album;

    @NotBlank(message = "Duration is required")
    @Pattern(regexp = "^[0-5]\\d:[0-5]\\d$", message = "Duration must represent mm:ss format (00:00 to 59:59)")
    String duration;

    @NotBlank(message = "Year is required")
    @Pattern(regexp = "^(19|20)\\d{2}$", message = "Year must be between 1900 and 2099 as 4 digits (e.g., '1999')")
    String year;
}
