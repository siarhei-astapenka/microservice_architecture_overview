package com.epam.learn.resource_processor.messaging;

import com.epam.learn.resource_processor.dto.ResourceUploadMessage;
import com.epam.learn.resource_processor.service.ResourceProcessorService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false",
        "spring.rabbitmq.dynamic=false",
        "management.health.rabbit.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.loadbalancer.enabled=false",
        "eureka.client.enabled=false"
})
@ActiveProfiles("test")
@DisplayName("ResourceUploadListener Integration Tests")
class ResourceUploadListenerIntegrationTest {

    @MockitoBean
    private ResourceProcessorService resourceProcessorService;

    @org.springframework.beans.factory.annotation.Autowired
    private ResourceUploadListener resourceUploadListener;

    @Test
    @DisplayName("Should ack message when processing succeeds")
    void handle_shouldAckOnSuccess() throws Exception {
        ResourceUploadMessage payload = ResourceUploadMessage.builder().resourceId(10L).build();
        Channel channel = mock(Channel.class);

        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(1L);
        Message amqpMessage = new Message(new byte[0], props);

        resourceUploadListener.handleResourceUploadMessage(payload, channel, amqpMessage);

        verify(resourceProcessorService).processResource(10L);
        verify(channel).basicAck(1L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("Should nack and requeue message when processing fails")
    void handle_shouldNackAndRequeueOnFailure() throws Exception {
        ResourceUploadMessage payload = ResourceUploadMessage.builder().resourceId(11L).build();
        Channel channel = mock(Channel.class);

        doThrow(new RuntimeException("boom")).when(resourceProcessorService).processResource(11L);

        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(2L);
        Message amqpMessage = new Message(new byte[0], props);

        resourceUploadListener.handleResourceUploadMessage(payload, channel, amqpMessage);

        verify(channel).basicNack(2L, false, true);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }
}
