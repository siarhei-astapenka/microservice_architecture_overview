package com.epam.learn.resource_service.cucumber;

import org.mockito.Mockito;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

/**
 * Test configuration that provides mock beans for tests.
 * Needed when Eureka and RabbitMQ are disabled in tests.
 */
@TestConfiguration
public class ResourceServiceTestConfig {

    @Bean
    @Primary
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    @Primary
    public ConnectionFactory connectionFactory() {
        return Mockito.mock(ConnectionFactory.class);
    }
}
