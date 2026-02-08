package com.epam.learn.resource_processor.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class ResourceServiceClient {

    private final RestTemplate restTemplate;

    @Value("${resource.service.url}")
    private String resourceServiceUrl;

    public ResourceServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Retryable(
            retryFor = {org.springframework.web.client.RestClientException.class},
            noRetryFor = {org.springframework.web.client.HttpClientErrorException.class},
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public byte[] getResourceData(Long resourceId) {
        String url = String.format("%s/resources/%d", resourceServiceUrl, resourceId);
        log.info("Fetching resource data from: {}", url);

        byte[] response = restTemplate.getForObject(url, byte[].class);
        log.info("Successfully fetched resource data for resourceId: {}", resourceId);

        return response;
    }
}
