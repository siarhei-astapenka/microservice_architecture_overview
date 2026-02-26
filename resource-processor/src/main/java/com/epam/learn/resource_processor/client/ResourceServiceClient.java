package com.epam.learn.resource_processor.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;

@Component
@Slf4j
public class ResourceServiceClient {

    private final RestTemplate restTemplate;

    public ResourceServiceClient(@Qualifier("resourceServiceRestTemplate") RestTemplate restTemplate) {
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
        String url = String.format("/resources/%d", resourceId);
        log.info("Fetching resource data from: {}", url);

        byte[] response = restTemplate.getForObject(url, byte[].class);
        log.info("Successfully fetched resource data for resourceId: {}", resourceId);

        return response;
    }

    public InputStream getResourceStream(Long resourceId) {
        String url = String.format("/resources/%d", resourceId);
        log.info("Fetching resource input stream from: {}", url);

        InputStream dataStream = restTemplate.execute(url, HttpMethod.GET, null, response ->
                response.getBody()
        );

        log.info("Successfully fetched resource input stream for resourceId: {}", resourceId);

        return dataStream;
    }

    @Recover
    public byte[] recoverGetResourceData(Exception e, Long resourceId) {
        log.error("Failed to fetch resource data after retries for resourceId={}: {}", resourceId, e.getMessage());
        e.printStackTrace();
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                "Resource service unavailable after retries", e);
    }
}
