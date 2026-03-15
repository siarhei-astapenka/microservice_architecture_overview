package com.epam.learn.resource_service.messaging;

import com.epam.learn.resource_service.model.ResourceUploadMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceUploadProducer {

    private final AmqpTemplate amqpTemplate;

    @Value("${rabbitmq.exchange.resource:resource.exchange}")
    private String resourceExchange;

    @Value("${rabbitmq.routing.key.resource.upload:resource.upload.routing.key}")
    private String resourceUploadRoutingKey;

    @Retryable(
            retryFor = {AmqpException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, maxDelay = 10000, multiplier = 2)
    )
    public void sendResourceUploadMessage(ResourceUploadMessage message) {
        log.info("Sending message for resourceId: {}", message.getResourceId());
        amqpTemplate.convertAndSend(resourceExchange, resourceUploadRoutingKey, message);
        log.info("Sent message for resourceId: {}", message.getResourceId());
    }

    @Recover
    public void recoverSendResourceUploadMessage(AmqpException e, ResourceUploadMessage message) {
        log.error("Failed to send message after retries for resourceId={}: {}",
                message.getResourceId(), e.getMessage());
        // Consider sending to DLQ or storing failed messages for later processing
    }
}
