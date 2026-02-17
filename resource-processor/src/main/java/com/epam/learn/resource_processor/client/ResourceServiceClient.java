package com.epam.learn.resource_processor.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
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
            retryFor = {RestClientException.class},
            noRetryFor = {
                    HttpClientErrorException.NotFound.class,
                    HttpClientErrorException.BadRequest.class,
                    HttpClientErrorException.Conflict.class,
                    HttpClientErrorException.UnprocessableEntity.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, maxDelay = 10000, multiplier = 2)
    )
    public byte[] getResourceData(Long resourceId) {
        String url = String.format("%s/resources/%d", resourceServiceUrl, resourceId);
        log.info("Fetching resource data from: {}", url);

        byte[] response = restTemplate.getForObject(url, byte[].class);
        log.info("Successfully fetched resource data for resourceId: {}", resourceId);

        return response;
    }

    @Recover
    public byte[] recoverGetResourceData(RestClientException e, Long resourceId) {
        log.error("Failed to fetch resource data after retries for resourceId={}: {}", resourceId, e.getMessage());
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                "Resource service unavailable after retries", e);
    }
}
