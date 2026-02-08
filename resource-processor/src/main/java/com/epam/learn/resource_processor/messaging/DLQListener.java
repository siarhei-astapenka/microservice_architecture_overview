package com.epam.learn.resource_processor.messaging;

import com.epam.learn.resource_processor.dto.ResourceUploadMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DLQListener {

    @RabbitListener(queues = "${rabbitmq.queue.resource.dlq:resource.upload.dlq}")
    public void handleDeadLetter(Message message) {
        ResourceUploadMessage uploadMessage = (ResourceUploadMessage) message.getMessageProperties().getHeaders().get("__TypeId__");
        
        log.error("Message sent to DLQ for resourceId: {}", 
                uploadMessage != null ? uploadMessage.getResourceId() : "unknown");
        log.error("Failed message body: {}", new String(message.getBody()));
        log.error("Message headers: {}", message.getMessageProperties().getHeaders());
    }
}
