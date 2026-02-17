package com.epam.learn.resource_processor.cucumber;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

/**
 * Test configuration that provides RestTemplate bean for tests.
 * Needed when Eureka is disabled in tests.
 */
@TestConfiguration
public class ResourceProcessorTestConfig {

    @Bean
    @Primary
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
