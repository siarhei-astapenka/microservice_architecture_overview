package com.epam.learn.resource_service.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class MetadataRestTemplateConfig {
    @Value("${song.metadata.service.url}")
    private String serviceUrl;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .rootUri(serviceUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}

