package com.epam.learn.resource_processor.configuration;

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
public class MetadataRestTemplateConfig {

    private final ServiceTokenProvider serviceTokenProvider;

    @Value("${song.service.url}")
    private String songServiceUrl;

    @Value("${resource.service.url}")
    private String resourceServiceUrl;

    @Bean("songServiceRestTemplate")
    @LoadBalanced
    @ConditionalOnProperty(name = "eureka.client.enabled", havingValue = "true", matchIfMissing = true)
    public RestTemplate songServiceRestTemplate(RestTemplateBuilder builder) {
        return builder
                .rootUri(songServiceUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + serviceTokenProvider.getServiceToken())
                .build();
    }

    @Bean("songServiceRestTemplate")
    @ConditionalOnProperty(name = "eureka.client.enabled", havingValue = "false")
    public RestTemplate songServiceRestTemplateNoDiscovery(RestTemplateBuilder builder) {
        return builder
                .rootUri(songServiceUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + serviceTokenProvider.getServiceToken())
                .build();
    }

    @Bean("resourceServiceRestTemplate")
    @LoadBalanced
    @ConditionalOnProperty(name = "eureka.client.enabled", havingValue = "true", matchIfMissing = true)
    public RestTemplate resourceServiceRestTemplate(RestTemplateBuilder builder) {
        return builder
                .rootUri(resourceServiceUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + serviceTokenProvider.getServiceToken())
                .build();
    }

    @Bean("resourceServiceRestTemplate")
    @ConditionalOnProperty(name = "eureka.client.enabled", havingValue = "false")
    public RestTemplate resourceServiceRestTemplateNoDiscovery(RestTemplateBuilder builder) {
        return builder
                .rootUri(resourceServiceUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + serviceTokenProvider.getServiceToken())
                .build();
    }
}
