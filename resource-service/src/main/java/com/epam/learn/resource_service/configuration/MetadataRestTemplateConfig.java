package com.epam.learn.resource_service.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class MetadataRestTemplateConfig {

    @Value("${song.service.url:http://localhost:8081}")
    private String songServiceUrl;

    @Bean
    @LoadBalanced
    @ConditionalOnProperty(name = "eureka.client.enabled", havingValue = "true", matchIfMissing = true)
    public RestTemplate loadBalancedRestTemplate(RestTemplateBuilder builder) {
        return builder
                .rootUri(songServiceUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}

