package com.epam.learn.resource_service.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {

    private final ServiceTokenProvider serviceTokenProvider;

    @Value("${song.service.url}")
    private String songServiceUrl;

    @Value("${storage.service.url}")
    private String storageServiceUrl;

    @Bean
    @LoadBalanced
    @ConditionalOnProperty(name = "eureka.client.enabled", havingValue = "true", matchIfMissing = true)
    public RestTemplate loadBalancedRestTemplate(RestTemplateBuilder builder) {
        return builder
                .rootUri(songServiceUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + serviceTokenProvider.getServiceToken())
                .build();
    }

    @Bean
    @LoadBalanced
    @ConditionalOnProperty(name = "eureka.client.enabled", havingValue = "true", matchIfMissing = true)
    public RestTemplate storageServiceRestTemplate(RestTemplateBuilder builder) {
        return builder
                .rootUri(storageServiceUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + serviceTokenProvider.getServiceToken())
                .build();
    }
}
