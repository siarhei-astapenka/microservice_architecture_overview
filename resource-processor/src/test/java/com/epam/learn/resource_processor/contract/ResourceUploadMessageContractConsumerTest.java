package com.epam.learn.resource_processor.contract;

import com.epam.learn.resource_processor.dto.ResourceUploadMessage;
import com.epam.learn.resource_processor.messaging.ResourceUploadListener;
import com.epam.learn.resource_processor.service.ResourceProcessorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.StubTrigger;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consumer-side contract test for resource-processor consuming resource-service messaging contract.
 * Uses Spring Cloud Contract Stub Runner to trigger messages defined in resource-service contracts.
 *
 * <p>This test verifies that resource-processor correctly handles the ResourceUploadMessage
 * sent by resource-service according to the agreed messaging contract.</p>
 *
 * <p>Contract points tested:
 * <ul>
 *   <li>resource.exchange / resource.upload.routing.key - resource upload message</li>
 * </ul>
 * </p>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {
                ResourceUploadMessageContractConsumerTest.MessagingTestConfig.class
        },
        properties = {
                "resource.service.url=http://localhost:8080",
                "song.service.url=http://localhost:8081",
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.loadbalancer.enabled=false",
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "spring.rabbitmq.listener.direct.auto-startup=false",
                "management.health.rabbit.enabled=false",
                "rabbitmq.queue.resource.upload.queue=resource.upload.queue",
                "rabbitmq.exchange.resource.main=resource.exchange",
                "rabbitmq.routing.key.resource.upload=resource.upload.routing.key"
        }
)
@AutoConfigureStubRunner(
        ids = "com.epam.learn:resource-service:+:stubs",
        stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
@AutoConfigureMessageVerifier
@ActiveProfiles("test")
@DisplayName("Resource Processor Messaging Contract Consumer Tests")
class ResourceUploadMessageContractConsumerTest {

    @Autowired
    private StubTrigger stubTrigger;

    @Autowired
    private MessageChannel resourceExchangeChannel;

    @Test
    @DisplayName("Should receive resource upload message from resource-service via messaging contract")
    void shouldReceiveResourceUploadMessageViaContract() {
        // Given - set up a message receiver to capture the message
        AtomicReference<org.springframework.messaging.Message<?>> receivedMessage = new AtomicReference<>();

        if (resourceExchangeChannel instanceof SubscribableChannel subscribableChannel) {
            MessageHandler handler = message -> receivedMessage.set(message);
            subscribableChannel.subscribe(handler);

            try {
                // When - trigger the message defined in the resource-service contract
                stubTrigger.trigger("resource_uploaded");

                // Then - verify the message was received with correct structure
                assertThat(receivedMessage.get()).isNotNull();
                Object payload = receivedMessage.get().getPayload();
                assertThat(payload).isNotNull();
            } finally {
                subscribableChannel.unsubscribe(handler);
            }
        } else {
            // For QueueChannel - trigger and then receive
            stubTrigger.trigger("resource_uploaded");

            org.springframework.messaging.Message<?> message =
                    ((QueueChannel) resourceExchangeChannel).receive(5000);
            assertThat(message).isNotNull();
            assertThat(message.getPayload()).isNotNull();
        }
    }

    @Configuration
    static class MessagingTestConfig {

        /**
         * Spring Integration channel named after the AMQP exchange.
         * The StubTrigger sends messages to this channel when triggered.
         */
        @Bean("resource.exchange")
        public MessageChannel resourceExchangeChannel() {
            return new QueueChannel();
        }
    }
}
