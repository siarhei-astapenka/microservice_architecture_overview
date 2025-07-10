package com.epam.learn.song_service.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Value
@Jacksonized
@Builder
@JsonPropertyOrder({"errorMessage", "details", "errorCode"})
public class ErrorResponse {
    String errorMessage;
    Map<String, String> details;
    String errorCode;
}

