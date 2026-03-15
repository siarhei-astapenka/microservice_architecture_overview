package com.epam.learn.resource_service.messaging;

import com.epam.learn.resource_service.model.ResourceUploadMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ResourceUploadProducer.
 * Note: @Retryable is not tested in these unit tests as it requires Spring context.
 * For full retry testing, integration tests with RabbitMQ are recommended.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceUploadProducer Unit Tests")
class ResourceUploadProducerTest {

    @Mock
    private AmqpTemplate amqpTemplate;

    private ResourceUploadProducer resourceUploadProducer;

    @BeforeEach
    void setUp() {
        resourceUploadProducer = new ResourceUploadProducer(amqpTemplate);
        // Inject private fields
        ReflectionTestUtils.setField(resourceUploadProducer, "resourceExchange", "resource.exchange");
        ReflectionTestUtils.setField(resourceUploadProducer, "resourceUploadRoutingKey", "resource.upload.routing.key");
    }

    @Test
    @DisplayName("Should send message to RabbitMQ successfully")
    void sendResourceUploadMessage_shouldSendSuccessfully() {
        // Given
        ResourceUploadMessage message = ResourceUploadMessage.builder()
                .resourceId(1L)
                .storageBucket("test-bucket")
                .storageKey("test-key")
                .build();

        // When
        resourceUploadProducer.sendResourceUploadMessage(message);

        // Then
        ArgumentCaptor<ResourceUploadMessage> messageCaptor = ArgumentCaptor.forClass(ResourceUploadMessage.class);
        verify(amqpTemplate).convertAndSend(
                eq("resource.exchange"),
                eq("resource.upload.routing.key"),
                messageCaptor.capture()
        );

        ResourceUploadMessage capturedMessage = messageCaptor.getValue();
        assertEquals(1L, capturedMessage.getResourceId());
        assertEquals("test-bucket", capturedMessage.getStorageBucket());
        assertEquals("test-key", capturedMessage.getStorageKey());
    }

    @Test
    @DisplayName("Should throw exception when AmqpTemplate fails")
    void sendResourceUploadMessage_shouldThrowOnAmqpException() {
        // Given
        ResourceUploadMessage message = ResourceUploadMessage.builder()
                .resourceId(1L)
                .storageBucket("test-bucket")
                .storageKey("test-key")
                .build();

        doThrow(new AmqpException("Connection refused"))
                .when(amqpTemplate).convertAndSend(any(String.class), any(String.class), any(ResourceUploadMessage.class));

        // When & Then
        assertThrows(AmqpException.class, () -> {
            resourceUploadProducer.sendResourceUploadMessage(message);
        });
    }

    @Test
    @DisplayName("Should handle message with null values")
    void sendResourceUploadMessage_shouldHandleNullValues() {
        // Given
        ResourceUploadMessage message = ResourceUploadMessage.builder()
                .resourceId(999L)
                .build();

        // When
        resourceUploadProducer.sendResourceUploadMessage(message);

        // Then
        verify(amqpTemplate).convertAndSend(
                eq("resource.exchange"),
                eq("resource.upload.routing.key"),
                any(ResourceUploadMessage.class)
        );
    }

    @Test
    @DisplayName("Should use correct exchange and routing key")
    void sendResourceUploadMessage_shouldUseCorrectExchangeAndRoutingKey() {
        // Given
        ResourceUploadMessage message = ResourceUploadMessage.builder()
                .resourceId(1L)
                .build();

        // When
        resourceUploadProducer.sendResourceUploadMessage(message);

        // Then - verify exact values passed
        verify(amqpTemplate).convertAndSend(
                "resource.exchange",
                "resource.upload.routing.key",
                message
        );
    }

    @Test
    @DisplayName("Should handle multiple messages with different IDs")
    void sendResourceUploadMessage_shouldHandleMultipleMessages() {
        // Given
        ResourceUploadMessage message1 = ResourceUploadMessage.builder()
                .resourceId(1L)
                .build();
        ResourceUploadMessage message2 = ResourceUploadMessage.builder()
                .resourceId(2L)
                .build();
        ResourceUploadMessage message3 = ResourceUploadMessage.builder()
                .resourceId(3L)
                .build();

        // When
        resourceUploadProducer.sendResourceUploadMessage(message1);
        resourceUploadProducer.sendResourceUploadMessage(message2);
        resourceUploadProducer.sendResourceUploadMessage(message3);

        // Then
        verify(amqpTemplate, times(3)).convertAndSend(
                any(String.class),
                any(String.class),
                any(ResourceUploadMessage.class)
        );
    }

    @Test
    @DisplayName("Should call recover method when AmqpException occurs")
    void recoverSendResourceUploadMessage_shouldLogError() {
        // Given
        ResourceUploadMessage message = ResourceUploadMessage.builder()
                .resourceId(123L)
                .storageBucket("test-bucket")
                .storageKey("test-key")
                .build();
        AmqpException exception = new AmqpException("Connection failed");

        // When - directly call recover method
        resourceUploadProducer.recoverSendResourceUploadMessage(exception, message);

        // Then - verify no exception is thrown (method logs error)
        // The method should complete without throwing an exception
        assertNotNull(message);
        assertEquals(123L, message.getResourceId());
    }

    @Test
    @DisplayName("Should throw NullPointerException when message is null in recover")
    void recoverSendResourceUploadMessage_shouldThrowNPEForNullMessage() {
        // Given
        AmqpException exception = new AmqpException("Connection failed");

        // When & Then - should throw NullPointerException because recover method accesses message.getResourceId()
        assertThrows(NullPointerException.class, () -> 
            resourceUploadProducer.recoverSendResourceUploadMessage(exception, null)
        );
    }

    @Test
    @DisplayName("Should send message with all fields populated")
    void sendResourceUploadMessage_shouldSendCompleteMessage() {
        // Given
        ResourceUploadMessage message = ResourceUploadMessage.builder()
                .resourceId(42L)
                .storageBucket("production-bucket")
                .storageKey("audio/2024/song.mp3")
                .build();

        // When
        resourceUploadProducer.sendResourceUploadMessage(message);

        // Then
        ArgumentCaptor<ResourceUploadMessage> captor = ArgumentCaptor.forClass(ResourceUploadMessage.class);
        verify(amqpTemplate).convertAndSend(
                eq("resource.exchange"),
                eq("resource.upload.routing.key"),
                captor.capture()
        );

        ResourceUploadMessage captured = captor.getValue();
        assertEquals(42L, captured.getResourceId());
        assertEquals("production-bucket", captured.getStorageBucket());
        assertEquals("audio/2024/song.mp3", captured.getStorageKey());
    }

    @Test
    @DisplayName("Should verify AmqpTemplate is called exactly once per message")
    void sendResourceUploadMessage_shouldCallAmqpTemplateOnce() {
        // Given
        ResourceUploadMessage message = ResourceUploadMessage.builder()
                .resourceId(1L)
                .build();

        // When
        resourceUploadProducer.sendResourceUploadMessage(message);

        // Then
        verify(amqpTemplate, times(1)).convertAndSend(
                any(String.class),
                any(String.class),
                any(ResourceUploadMessage.class)
        );
        verifyNoMoreInteractions(amqpTemplate);
    }
}
