package com.epam.learn.resource_service.client;

import com.epam.learn.resource_service.model.Storage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Fallback provider for Storage Service client.
 * This provides default/fallback values when the Storage Service is unavailable,
 * implementing the circuit breaker pattern for fault tolerance.
 */
@Component
@Slf4j
public class StorageFallbackProvider {

    /**
     * Provides a fallback Storage for STAGING type when Storage Service is unavailable.
     * Uses near-static data from configuration to emulate stubbed behavior.
     */
    public Optional<Storage> getStagingStorageFallback() {
        log.warn("Storage Service unavailable - using STAGING fallback configuration");
        // Return default staging storage values
        // These values simulate near-static data when Storage Service is down
        return Optional.of(Storage.builder()
                .id(-1L)
                .storageType("STAGING")
                .bucket("staging-bucket")
                .path("/files")
                .build());
    }

    /**
     * Provides a fallback Storage for PERMANENT type when Storage Service is unavailable.
     * Uses near-static data from configuration to emulate stubbed behavior.
     */
    public Optional<Storage> getPermanentStorageFallback() {
        log.warn("Storage Service unavailable - using PERMANENT fallback configuration");
        // Return default permanent storage values
        // These values simulate near-static data when Storage Service is down
        return Optional.of(Storage.builder()
                .id(-2L)
                .storageType("PERMANENT")
                .bucket("permanent-bucket")
                .path("/files")
                .build());
    }
}
