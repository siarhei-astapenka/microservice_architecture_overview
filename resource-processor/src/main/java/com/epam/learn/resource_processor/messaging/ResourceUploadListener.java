package com.epam.learn.resource_processor.messaging;

import com.epam.learn.resource_processor.dto.ResourceUploadMessage;
import com.epam.learn.resource_processor.service.ResourceProcessorService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResourceUploadListener {

    private final ResourceProcessorService resourceProcessorService;

    @RabbitListener(queues = "${rabbitmq.queue.resource.upload.queue}")
    public void handleResourceUploadMessage(ResourceUploadMessage message,
                                           Channel channel,
                                           org.springframework.amqp.core.Message amqpMessage) {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        log.info("Received resource upload message: {}", message);

        try {
            // Process the resource
            resourceProcessorService.processResource(message.getResourceId());

            // Acknowledge successful processing
            channel.basicAck(deliveryTag, false);
            log.info("Successfully processed and acknowledged message for resourceId: {}",
                    message.getResourceId());

        } catch (Exception e) {
            log.error("Error processing message for resourceId: {}. Error: {}",
                    message.getResourceId(), e.getMessage());

            try {
                // Send to DLQ instead of requeue to prevent infinite loop
                // Set requeue to false - message will go to dead letter queue
                channel.basicNack(deliveryTag, false, false);
                log.info("Message rejected and sent to DLQ for resourceId: {}", message.getResourceId());
            } catch (Exception ex) {
                log.error("Error rejecting message for resourceId: {}. Error: {}",
                        message.getResourceId(), ex.getMessage());
            }
        }
    }
}
