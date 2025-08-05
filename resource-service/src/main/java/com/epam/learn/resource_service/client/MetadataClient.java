package com.epam.learn.resource_service.client;

import com.epam.learn.resource_service.model.metadata.MetadataRequest;
import com.epam.learn.resource_service.model.metadata.MetadataResponse;
import lombok.AllArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.ws.rs.ServiceUnavailableException;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class MetadataClient {

    private final RestTemplate restTemplate;

    public MetadataResponse postMetadata(MetadataRequest request) {
        ResponseEntity<MetadataResponse> response = restTemplate.postForEntity(
                "http://song-service/songs",
                request,
                MetadataResponse.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new ResponseStatusException(response.getStatusCode(),
                        "Metadata service returned status: " + response.getStatusCode());
        }

        return response.getBody();
    }

    public Map<String, List<Long>> deleteMetadata(String commaSeparatedIds) {
        try {
            ResponseEntity<Map<String, List<Long>>> response = restTemplate.exchange(
                    "http://song-service/songs?id={ids}",
                    HttpMethod.DELETE,
                    null,
                    new ParameterizedTypeReference<>() {},
                    commaSeparatedIds
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ResponseStatusException(
                        response.getStatusCode(),
                        "Metadata service returned status: " + response.getStatusCode()
                );
            }

            return response.getBody();
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("Metadata service unavailable");
        }
    }
}
