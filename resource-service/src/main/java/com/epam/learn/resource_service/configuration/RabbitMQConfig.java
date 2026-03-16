package com.epam.learn.resource_service.configuration;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.queue.resource.upload.queue:resource.upload.queue}")
    private String resourceUploadQueue;

    @Value("${rabbitmq.exchange.resource.main:resource.exchange}")
    private String resourceExchange;

    @Value("${rabbitmq.routing.key.resource.upload:resource.upload.routing.key}")
    private String resourceUploadRoutingKey;

    @Value("${rabbitmq.queue.resource.upload.dlq:resource.upload.dlq}")
    private String resourceUploadDLQ;

    @Value("${rabbitmq.exchange.resource.dlx:resource.dlx}")
    private String resourceDLX;

    @Value("${rabbitmq.routing.key.resource.dlq:resource.upload.routing.key}")
    private String resourceDLQRoutingKey;

    // Resource processed queue configuration
    @Value("${rabbitmq.queue.resource.processed.queue:resource.processed.queue}")
    private String resourceProcessedQueue;

    @Value("${rabbitmq.routing.key.resource.processed:resource.processed.routing.key}")
    private String resourceProcessedRoutingKey;

    @Bean
    public Queue resourceUploadQueue() {
        return QueueBuilder.durable(resourceUploadQueue)
                .withArgument("x-dead-letter-exchange", resourceDLX)
                .withArgument("x-dead-letter-routing-key", resourceDLQRoutingKey)
                .build();
    }

    @Bean
    public Queue resourceUploadDLQ() {
        return QueueBuilder.durable(resourceUploadDLQ).build();
    }

    @Bean
    public Queue resourceProcessedQueue() {
        return QueueBuilder.durable(resourceProcessedQueue)
                .withArgument("x-dead-letter-exchange", resourceDLX)
                .withArgument("x-dead-letter-routing-key", resourceDLQRoutingKey)
                .build();
    }

    @Bean
    public TopicExchange resourceExchange() {
        return new TopicExchange(resourceExchange);
    }

    @Bean
    public DirectExchange resourceDLX() {
        return new DirectExchange(resourceDLX);
    }

    @Bean
    public Binding resourceUploadBinding() {
        return BindingBuilder.bind(resourceUploadQueue())
                .to(resourceExchange())
                .with(resourceUploadRoutingKey);
    }

    @Bean
    public Binding resourceProcessedBinding() {
        return BindingBuilder.bind(resourceProcessedQueue())
                .to(resourceExchange())
                .with(resourceProcessedRoutingKey);
    }

    @Bean
    public Binding resourceUploadDLQBinding() {
        return BindingBuilder.bind(resourceUploadDLQ())
                .to(resourceDLX())
                .with(resourceDLQRoutingKey);
    }

    @Bean
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    @Primary
    public AmqpTemplate template(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }
}
