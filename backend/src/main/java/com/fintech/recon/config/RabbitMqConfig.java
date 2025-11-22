package com.fintech.recon.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE_TRANSACTION = "transaction.exchange";
    public static final String QUEUE_RECONCILIATION = "reconciliation.queue";
    public static final String ROUTING_KEY_INGESTED = "transaction.ingested";
    
    public static final String EXCHANGE_WEBHOOK_DLX = "webhook.dlx";
    public static final String QUEUE_WEBHOOK_DLQ = "webhook.dlq";

    @Bean
    public TopicExchange transactionExchange() {
        return new TopicExchange(EXCHANGE_TRANSACTION);
    }

    @Bean
    public Queue reconciliationQueue() {
        return QueueBuilder.durable(QUEUE_RECONCILIATION)
                .build();
    }

    @Bean
    public Binding binding(Queue reconciliationQueue, TopicExchange transactionExchange) {
        return BindingBuilder.bind(reconciliationQueue)
                .to(transactionExchange)
                .with(ROUTING_KEY_INGESTED);
    }

    // DLQ Configuration for Webhooks
    @Bean
    public TopicExchange webhookDlx() {
        return new TopicExchange(EXCHANGE_WEBHOOK_DLX);
    }

    @Bean
    public Queue webhookDlq() {
        return QueueBuilder.durable(QUEUE_WEBHOOK_DLQ).build();
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
