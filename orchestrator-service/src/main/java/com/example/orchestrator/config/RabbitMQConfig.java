package com.example.orchestrator.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;
    
    @Value("${rabbitmq.queue.extraction-requests}")
    private String extractionRequestsQueue;
    
    @Value("${rabbitmq.queue.extraction-completed}")
    private String extractionCompletedQueue;
    
    @Value("${rabbitmq.queue.indexing-requests}")
    private String indexingRequestsQueue;
    
    @Value("${rabbitmq.queue.indexing-completed}")
    private String indexingCompletedQueue;
    
    @Value("${rabbitmq.routing-key.extraction-request}")
    private String extractionRequestKey;
    
    @Value("${rabbitmq.routing-key.extraction-completed}")
    private String extractionCompletedKey;
    
    @Value("${rabbitmq.routing-key.indexing-request}")
    private String indexingRequestKey;
    
    @Value("${rabbitmq.routing-key.indexing-completed}")
    private String indexingCompletedKey;
    
    // Exchange
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchangeName);
    }
    
    // Queues
    @Bean
    public Queue extractionRequestsQueue() {
        return QueueBuilder.durable(extractionRequestsQueue)
                .withArgument("x-message-ttl", 300000) // 5 minutes
                .build();
    }
    
    @Bean
    public Queue extractionCompletedQueue() {
        return new Queue(extractionCompletedQueue, true);
    }
    
    @Bean
    public Queue indexingRequestsQueue() {
        return QueueBuilder.durable(indexingRequestsQueue)
                .withArgument("x-message-ttl", 300000)
                .build();
    }
    
    @Bean
    public Queue indexingCompletedQueue() {
        return new Queue(indexingCompletedQueue, true);
    }
    
    // Bindings
    @Bean
    public Binding extractionRequestBinding() {
        return BindingBuilder
                .bind(extractionRequestsQueue())
                .to(exchange())
                .with(extractionRequestKey);
    }
    
    @Bean
    public Binding extractionCompletedBinding() {
        return BindingBuilder
                .bind(extractionCompletedQueue())
                .to(exchange())
                .with(extractionCompletedKey);
    }
    
    @Bean
    public Binding indexingRequestBinding() {
        return BindingBuilder
                .bind(indexingRequestsQueue())
                .to(exchange())
                .with(indexingRequestKey);
    }
    
    @Bean
    public Binding indexingCompletedBinding() {
        return BindingBuilder
                .bind(indexingCompletedQueue())
                .to(exchange())
                .with(indexingCompletedKey);
    }
    
    // Message Converter
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    // RabbitTemplate
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
