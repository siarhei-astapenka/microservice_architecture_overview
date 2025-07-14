package com.epam.learn.resource_service.model.metadata;

import lombok.Builder;
import lombok.Value;

import java.time.Duration;
import java.time.LocalDate;

@Value
@Builder
public class Metadata {
    String name;
    String artist;
    String album;
    Duration duration;
    LocalDate year;
}
