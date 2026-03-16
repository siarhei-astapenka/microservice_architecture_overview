package com.epam.learn.resource_service.client;

import com.epam.learn.resource_service.enumeration.StorageType;
import com.epam.learn.resource_service.model.Storage;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Client for communicating with the Storage Service.
 * Implements circuit breaker pattern for fault tolerance.
 */
@Component
@Slf4j
public class StorageServiceClient {

    private final RestTemplate restTemplate;
    private final StorageFallbackProvider fallbackProvider;

    public StorageServiceClient(
            @Qualifier("storageServiceRestTemplate") RestTemplate restTemplate,
            StorageFallbackProvider fallbackProvider) {
        this.restTemplate = restTemplate;
        this.fallbackProvider = fallbackProvider;
    }

    private static final String CIRCUIT_BREAKER_NAME = "storageServiceCircuitBreaker";

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getStorageByTypeFallback")
    public Optional<Storage> getStorageByType(StorageType storageType) {
        try {
            var response = restTemplate.exchange(
                    "/storages",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Storage>>() {}
            );

            List<Storage> storages = response.getBody();
            if (storages != null) {
                return storages.stream()
                        .filter(s -> storageType.getValue().equals(s.getStorageType()))
                        .findFirst();
            }
            return Optional.empty();
        } catch (RestClientException e) {
            log.error("Failed to get storages from storage service: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Fallback method for getStorageByType when circuit breaker is open or call fails.
     * Provides near-static fallback data to ensure continuous operation.
     */
    public Optional<Storage> getStorageByTypeFallback(StorageType storageType, Throwable t) {
        log.warn("Circuit breaker fallback triggered for storageType={}, reason: {}", 
                storageType, t.getMessage());
        
        // Check if this is a circuit breaker open state
        if (t instanceof CallNotPermittedException) {
            log.warn("Circuit breaker is OPEN for Storage Service, using fallback data");
        }
        
        // Return fallback based on storage type
        if (StorageType.STAGING.equals(storageType)) {
            return fallbackProvider.getStagingStorageFallback();
        } else if (StorageType.PERMANENT.equals(storageType)) {
            return fallbackProvider.getPermanentStorageFallback();
        }
        
        return Optional.empty();
    }

    public Storage createStorage(Storage storage) {
        try {
            var response = restTemplate.postForEntity(
                    "/storages",
                    storage,
                    Storage.class
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to create storage: {}", e.getMessage());
            throw e;
        }
    }
}
