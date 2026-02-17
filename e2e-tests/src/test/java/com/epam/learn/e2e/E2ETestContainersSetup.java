package com.epam.learn.e2e;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Service URL resolver for the E2E test suite.
 *
 * <p>Assumes the full microservice stack is already running (started manually via
 * {@code docker compose up} before the test suite is executed). Service URLs are
 * resolved from {@code e2e-config.properties} configuration file.
 *
 * <h3>Default port mappings (from docker-compose.yml)</h3>
 * <ul>
 *   <li>resource-service  – {@code http://localhost:8080}</li>
 *   <li>song-service      – {@code http://localhost:8081}</li>
 *   <li>resource-processor – {@code http://localhost:8082}</li>
 *   <li>RabbitMQ management – {@code http://localhost:15672}</li>
 * </ul>
 *
 * <h3>Configuration file (e2e-config.properties)</h3>
 * <pre>
 *   resource.service.url=http://localhost:8080
 *   song.service.url=http://localhost:8081
 *   resource.processor.url=http://localhost:8082
 *   rabbitmq.management.url=http://localhost:15672
 * </pre>
 */
public final class E2ETestContainersSetup {

    private static final Logger log = LoggerFactory.getLogger(E2ETestContainersSetup.class);

    private static final Properties properties = new Properties();

    static {
        try (InputStream input = E2ETestContainersSetup.class.getClassLoader()
                .getResourceAsStream("e2e-config.properties")) {
            if (input == null) {
                throw new RuntimeException("Unable to find e2e-config.properties");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load e2e-config.properties", e);
        }
    }

    private static final String RESOURCE_SERVICE_URL =
            properties.getProperty("resource.service.url", "http://localhost:8080");
    private static final String SONG_SERVICE_URL =
            properties.getProperty("song.service.url", "http://localhost:8081");
    private static final String RESOURCE_PROCESSOR_URL =
            properties.getProperty("resource.processor.url", "http://localhost:8082");
    private static final String RABBITMQ_MGMT_URL =
            properties.getProperty("rabbitmq.management.url", "http://localhost:15672");

    private E2ETestContainersSetup() {
        // utility class
    }

    /**
     * No-op: the stack is started manually before the test suite runs.
     * This method exists so {@link E2ECucumberHooks} can call it without change.
     */
    public static void start() {
        log.info("E2E suite: using pre-started stack – resource-service={}, song-service={}, resource-processor={}",
                RESOURCE_SERVICE_URL, SONG_SERVICE_URL, RESOURCE_PROCESSOR_URL);
    }

    // ── URL accessors ─────────────────────────────────────────────────────────

    public static String getResourceServiceUrl() {
        return RESOURCE_SERVICE_URL;
    }

    public static String getSongServiceUrl() {
        return SONG_SERVICE_URL;
    }

    public static String getResourceProcessorUrl() {
        return RESOURCE_PROCESSOR_URL;
    }

    public static String getRabbitMQManagementUrl() {
        return RABBITMQ_MGMT_URL;
    }
}
