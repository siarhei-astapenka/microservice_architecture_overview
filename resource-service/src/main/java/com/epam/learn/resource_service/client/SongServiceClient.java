package com.epam.learn.resource_service.client;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
@AllArgsConstructor
public class SongServiceClient {

    private final RestTemplate restTemplate;

    @Retryable(
            retryFor = {RestClientException.class},
            noRetryFor = {HttpClientErrorException.NotFound.class, HttpClientErrorException.BadRequest.class},
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public Map<String, List<Long>> deleteMetadata(String commaSeparatedIds) {
        log.info("Calling song-service DELETE for ids={}", commaSeparatedIds);

        var response = restTemplate.exchange(
                "/songs?id={ids}",
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<Map<String, List<Long>>>() {},
                commaSeparatedIds
        );

        return response.getBody();
    }

    @Recover
    public Map<String, List<Long>> recoverDeleteMetadata(RestClientException e, String commaSeparatedIds) {
        log.error("Failed to delete metadata after retries for ids={}: {}", commaSeparatedIds, e.getMessage());
        return Map.of("ids", List.of());
    }
}
