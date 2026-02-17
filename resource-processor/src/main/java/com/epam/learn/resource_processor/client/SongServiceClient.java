package com.epam.learn.resource_processor.client;

import com.epam.learn.resource_processor.dto.SongMetadataRequest;
import com.epam.learn.resource_processor.dto.SongMetadataResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Component
@Slf4j
@AllArgsConstructor
public class SongServiceClient {

    private final RestTemplate restTemplate;

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
    public SongMetadataResponse saveSongMetadata(SongMetadataRequest request) {
        ResponseEntity<SongMetadataResponse> response = restTemplate.postForEntity(
                "/songs",
                request,
                SongMetadataResponse.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new ResponseStatusException(response.getStatusCode(),
                    "Metadata service returned status: " + response.getStatusCode());
        }

        log.info("Successfully saved song metadata. Response: {}", response.getBody());

        return response.getBody();
    }

    @Recover
    public SongMetadataResponse recoverSaveSongMetadata(RestClientException e, SongMetadataRequest request) {
        log.error("Failed to save song metadata after retries. Request: {}. Error: {}", request, e.getMessage());
        throw new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                "Metadata service unavailable after retries", e);
    }
}
