package com.epam.learn.song_service.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Map;

@Value.Immutable
@JsonDeserialize(as = ImmutableErrorResponse.class)
@JsonPropertyOrder({
        "errorMessage", "details", "errorCode"
})
public interface ErrorResponse {
    String errorMessage();
    Map<String, String> details();
    String errorCode();
}

