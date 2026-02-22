package com.epam.learn.resource_service.contract;

import com.epam.learn.resource_service.dto.ResourceUploadMessage;
import com.epam.learn.resource_service.messaging.ResourceUploadProducer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

/**
 * Base class for Spring Cloud Contract producer tests in resource-service (messaging contracts).
 * Tests that resource-service correctly sends messages to RabbitMQ exchange.
 *
 * <p>Uses Spring Integration channels to bridge AMQP message sending to the
 * Spring Cloud Contract messaging verifier infrastructure.</p>
 */
@SpringBootTest(
        classes = {
                ResourceUploadProducer.class,
                ResourceServiceMessagingContractBase.TestConfig.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.loadbalancer.enabled=false",
                "rabbitmq.exchange.resource=resource.exchange",
                "rabbitmq.routing.key.resource.upload=resource.upload.routing.key"
        }
)
@AutoConfigureMessageVerifier
@EnableAutoConfiguration(exclude = {RabbitAutoConfiguration.class})
@ActiveProfiles("test")
public abstract class ResourceServiceMessagingContractBase {

    @Autowired
    private ResourceUploadProducer resourceUploadProducer;

    @MockitoBean
    private AmqpTemplate amqpTemplate;

    @Autowired
    @Qualifier("resource.exchange")
    private MessageChannel resourceExchangeChannel;

    @BeforeEach
    public void setup() {
        // When amqpTemplate.convertAndSend is called, forward the message to the Spring Integration channel
        // so that ContractVerifierMessaging can receive it
        doAnswer(invocation -> {
            Object payload = invocation.getArgument(2);
            org.springframework.messaging.support.MessageBuilder<?> builder =
                    org.springframework.messaging.support.MessageBuilder.withPayload(payload)
                            .setHeader("contentType", "application/json")
                            .setHeader("amqp_receivedRoutingKey", "resource.upload.routing.key");
            resourceExchangeChannel.send(builder.build());
            return null;
        }).when(amqpTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    /**
     * Trigger method called by the generated contract test.
     * Simulates the resource upload event that triggers message sending.
     */
    public void triggerResourceUploadMessage() {
        ResourceUploadMessage message = ResourceUploadMessage.builder()
                .resourceId(1L)
                .storageBucket("resource-bucket")
                .storageKey("resources/1")
                .build();
        resourceUploadProducer.sendResourceUploadMessage(message);
    }

    @Configuration
    @Import(ResourceUploadProducer.class)
    static class TestConfig {

        /**
         * Spring Integration QueueChannel named after the AMQP exchange.
         * ContractVerifierMessaging uses this channel to receive messages.
         * QueueChannel buffers messages so they can be received after being sent.
         */
        @Bean("resource.exchange")
        public MessageChannel resourceExchangeChannel() {
            return new QueueChannel();
        }
    }
}
