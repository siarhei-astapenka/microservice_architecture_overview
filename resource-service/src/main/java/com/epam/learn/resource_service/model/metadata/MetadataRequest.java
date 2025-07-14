package com.epam.learn.resource_service.model.metadata;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder
@JsonPropertyOrder({
        "id", "name", "artist", "album", "duration", "year"
})
public class MetadataRequest {
    Long id;
    String name;
    String artist;
    String album;
    String duration;
    String year;
}
