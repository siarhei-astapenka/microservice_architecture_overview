package com.epam.learn.song_service.configuration;

import com.epam.learn.song_service.model.serialization.SongDurationDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {

        SimpleModule durationModule = new SimpleModule();
        durationModule.addDeserializer(Duration.class, new SongDurationDeserializer());

        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .addModule(durationModule)
                .build()
                .enable(SerializationFeature.INDENT_OUTPUT);
    }
}