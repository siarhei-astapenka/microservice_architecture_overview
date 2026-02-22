package com.epam.learn.e2e;

import io.cucumber.java.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cucumber global hooks for the E2E test suite.
 *
 * <p>{@link BeforeAll} starts the Docker Compose stack exactly once before any
 * scenario runs. The stack is torn down by the JVM shutdown hook registered in
 * {@link E2ETestContainersSetup#start()}.
 */
public class E2ECucumberHooks {

    private static final Logger log = LoggerFactory.getLogger(E2ECucumberHooks.class);

    /**
     * Starts the full microservice stack before the first scenario.
     * Subsequent calls are no-ops (idempotent).
     */
    @BeforeAll
    public static void startStack() {
        log.info("E2E suite: starting microservice stack …");
        E2ETestContainersSetup.start();
        log.info("E2E suite: stack is ready");
    }
}
