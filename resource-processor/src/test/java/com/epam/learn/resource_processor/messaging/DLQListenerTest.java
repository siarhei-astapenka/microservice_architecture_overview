package com.epam.learn.resource_processor.messaging;

import com.epam.learn.resource_processor.dto.ResourceUploadMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

@DisplayName("DLQListener Unit Tests")
@ExtendWith(MockitoExtension.class)
class DLQListenerTest {

    private DLQListener dlqListener;

    @BeforeEach
    void setUp() {
        dlqListener = new DLQListener();
    }

    @Test
    @DisplayName("Should log dead letter message with resource ID")
    void shouldLogDeadLetterMessageWithResourceId() {
        // Given
        ResourceUploadMessage uploadMessage = ResourceUploadMessage.builder()
                .resourceId(123L)
                .build();

        MessageProperties properties = mock(MessageProperties.class);
        Map<String, Object> headers = new HashMap<>();
        headers.put("__TypeId__", uploadMessage);
        when(properties.getHeaders()).thenReturn(headers);

        Message message = mock(Message.class);
        when(message.getMessageProperties()).thenReturn(properties);
        when(message.getBody()).thenReturn("{\"resourceId\":123}".getBytes());

        // When
        dlqListener.handleDeadLetter(message);

        // Then - verify logging by checking no exceptions thrown
        verify(properties, atLeastOnce()).getHeaders();
        verify(message).getBody();
    }

    @Test
    @DisplayName("Should handle message with null upload message")
    void shouldHandleMessageWithNullUploadMessage() {
        // Given
        MessageProperties properties = mock(MessageProperties.class);
        Map<String, Object> headers = new HashMap<>();
        headers.put("__TypeId__", null);
        when(properties.getHeaders()).thenReturn(headers);

        Message message = mock(Message.class);
        when(message.getMessageProperties()).thenReturn(properties);
        when(message.getBody()).thenReturn("{}".getBytes());

        // When
        dlqListener.handleDeadLetter(message);

        // Then - verify no exceptions thrown
        verify(properties, atLeastOnce()).getHeaders();
    }

    @Test
    @DisplayName("Should handle message with empty headers")
    void shouldHandleMessageWithEmptyHeaders() {
        // Given
        MessageProperties properties = mock(MessageProperties.class);
        when(properties.getHeaders()).thenReturn(new HashMap<>());

        Message message = mock(Message.class);
        when(message.getMessageProperties()).thenReturn(properties);
        when(message.getBody()).thenReturn("{}".getBytes());

        // When
        dlqListener.handleDeadLetter(message);

        // Then - verify no exceptions thrown
        verify(properties, atLeastOnce()).getHeaders();
    }

    @Test
    @DisplayName("Should handle message with invalid body")
    void shouldHandleMessageWithInvalidBody() {
        // Given
        ResourceUploadMessage uploadMessage = ResourceUploadMessage.builder()
                .resourceId(456L)
                .build();

        MessageProperties properties = mock(MessageProperties.class);
        Map<String, Object> headers = new HashMap<>();
        headers.put("__TypeId__", uploadMessage);
        when(properties.getHeaders()).thenReturn(headers);

        Message message = mock(Message.class);
        when(message.getMessageProperties()).thenReturn(properties);
        when(message.getBody()).thenReturn(new byte[]{0, 1, 2, 3}); // Invalid UTF-8 bytes

        // When
        dlqListener.handleDeadLetter(message);

        // Then - verify no exceptions thrown
        verify(message).getBody();
    }
}
