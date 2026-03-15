package com.epam.learn.resource_service.messaging;

import com.epam.learn.resource_service.model.ResourceProcessedMessage;
import com.epam.learn.resource_service.service.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listener for processing completion notifications from Resource Processor.
 * When a resource has been successfully processed, this listener handles
 * moving the file from STAGING to PERMANENT storage.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResourceProcessedListener {

    private final ResourceService resourceService;

    @RabbitListener(queues = "${rabbitmq.queue.resource.processed.queue:resource.processed.queue}")
    public void handleResourceProcessed(ResourceProcessedMessage message) {
        log.info("Received processing complete message for resourceId: {}", message.getResourceId());

        try {
            // Handle the processing completion
            resourceService.handleProcessingComplete(message.getResourceId());
            log.info("Successfully processed completion for resourceId: {}", message.getResourceId());
        } catch (Exception e) {
            log.error("Error handling processing complete for resourceId={}: {}", 
                    message.getResourceId(), e.getMessage());
            throw e; // Let RabbitMQ handle retry/DLQ
        }
    }
}
