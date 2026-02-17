package com.epam.learn.resource_processor.messaging;

import com.epam.learn.resource_processor.dto.ResourceUploadMessage;
import com.epam.learn.resource_processor.service.ResourceProcessorService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@DisplayName("ResourceUploadListener Unit Tests")
@ExtendWith(MockitoExtension.class)
class ResourceUploadListenerTest {

    @Mock
    private ResourceProcessorService resourceProcessorService;

    @Mock
    private Channel channel;

    @Mock
    private Message amqpMessage;

    @Mock
    private MessageProperties messageProperties;

    private ResourceUploadListener resourceUploadListener;

    @BeforeEach
    void setUp() {
        resourceUploadListener = new ResourceUploadListener(resourceProcessorService);
    }

    @Test
    @DisplayName("Should process message and acknowledge successfully")
    void shouldProcessAndAcknowledge() throws Exception {
        // Given
        Long resourceId = 1L;
        ResourceUploadMessage message = ResourceUploadMessage.builder()
                .resourceId(resourceId)
                .build();
        long deliveryTag = 1L;

        when(amqpMessage.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getDeliveryTag()).thenReturn(deliveryTag);
        doNothing().when(resourceProcessorService).processResource(resourceId);
        doNothing().when(channel).basicAck(deliveryTag, false);

        // When
        resourceUploadListener.handleResourceUploadMessage(message, channel, amqpMessage);

        // Then
        verify(resourceProcessorService).processResource(resourceId);
        verify(channel).basicAck(deliveryTag, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("Should nack and requeue on processing error")
    void shouldNackAndRequeueOnError() throws Exception {
        // Given
        Long resourceId = 2L;
        ResourceUploadMessage message = ResourceUploadMessage.builder()
                .resourceId(resourceId)
                .build();
        long deliveryTag = 2L;

        when(amqpMessage.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getDeliveryTag()).thenReturn(deliveryTag);
        doThrow(new RuntimeException("Processing failed"))
                .when(resourceProcessorService).processResource(resourceId);
        doNothing().when(channel).basicNack(deliveryTag, false, true);

        // When
        resourceUploadListener.handleResourceUploadMessage(message, channel, amqpMessage);

        // Then
        verify(resourceProcessorService).processResource(resourceId);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(channel).basicNack(deliveryTag, false, true);
    }

    @Test
    @DisplayName("Should handle nack failure gracefully")
    void shouldHandleNackFailure() throws Exception {
        // Given
        Long resourceId = 3L;
        ResourceUploadMessage message = ResourceUploadMessage.builder()
                .resourceId(resourceId)
                .build();
        long deliveryTag = 3L;

        when(amqpMessage.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getDeliveryTag()).thenReturn(deliveryTag);
        doThrow(new RuntimeException("Processing failed"))
                .when(resourceProcessorService).processResource(resourceId);
        doThrow(new IOException("Nack failed"))
                .when(channel).basicNack(deliveryTag, false, true);

        // When
        resourceUploadListener.handleResourceUploadMessage(message, channel, amqpMessage);

        // Then - should not throw exception, just log error
        verify(resourceProcessorService).processResource(resourceId);
        verify(channel).basicNack(deliveryTag, false, true);
    }

    @Test
    @DisplayName("Should handle multiple messages sequentially")
    void shouldHandleMultipleMessages() throws Exception {
        // Given
        long deliveryTag = 10L;

        when(amqpMessage.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getDeliveryTag()).thenReturn(deliveryTag);
        doNothing().when(resourceProcessorService).processResource(anyLong());
        doNothing().when(channel).basicAck(anyLong(), anyBoolean());

        // When
        for (long i = 1; i <= 3; i++) {
            ResourceUploadMessage msg = ResourceUploadMessage.builder()
                    .resourceId(i)
                    .build();
            resourceUploadListener.handleResourceUploadMessage(msg, channel, amqpMessage);
        }

        // Then
        verify(resourceProcessorService, times(3)).processResource(anyLong());
        verify(channel, times(3)).basicAck(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("Should process message with null resourceId")
    void shouldHandleNullResourceId() throws Exception {
        // Given
        ResourceUploadMessage message = ResourceUploadMessage.builder()
                .resourceId(null)
                .build();
        long deliveryTag = 5L;

        when(amqpMessage.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getDeliveryTag()).thenReturn(deliveryTag);
        doNothing().when(resourceProcessorService).processResource(null);
        doNothing().when(channel).basicAck(deliveryTag, false);

        // When
        resourceUploadListener.handleResourceUploadMessage(message, channel, amqpMessage);

        // Then
        verify(resourceProcessorService).processResource(null);
        verify(channel).basicAck(deliveryTag, false);
    }

    @Test
    @DisplayName("Should handle processing service throwing runtime exception")
    void shouldHandleRuntimeException() throws Exception {
        // Given
        Long resourceId = 6L;
        ResourceUploadMessage message = ResourceUploadMessage.builder()
                .resourceId(resourceId)
                .build();
        long deliveryTag = 6L;

        when(amqpMessage.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getDeliveryTag()).thenReturn(deliveryTag);
        doThrow(new NullPointerException("Null pointer"))
                .when(resourceProcessorService).processResource(resourceId);
        doNothing().when(channel).basicNack(deliveryTag, false, true);

        // When
        resourceUploadListener.handleResourceUploadMessage(message, channel, amqpMessage);

        // Then
        verify(channel).basicNack(deliveryTag, false, true);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }
}
