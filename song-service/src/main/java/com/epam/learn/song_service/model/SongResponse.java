package com.epam.learn.song_service.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({
        "id", "name", "artist", "album", "duration", "year"
})
public class SongResponse {
    Long id;
    String name;
    String artist;
    String album;
    String duration;
    String year;
}
