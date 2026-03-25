package com.vinicius.vanguarda.infrastructure.messaging;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${vcb.rabbitmq.exchange:vcb.transactions}")
    private String exchange;

    @Value("${vcb.rabbitmq.dlx:vcb.transactions.dlx}")
    private String dlx;

    @Bean
    public TopicExchange vcbTransactionsExchange() {
        return ExchangeBuilder.topicExchange(exchange).durable(true).build();
    }

    @Bean
    public TopicExchange vcbTransactionsDlx() {
        return ExchangeBuilder.topicExchange(dlx).durable(true).build();
    }

    @Bean
    public Queue vcbTransactionCompletedQueue() {
        return QueueBuilder.durable("vcb.transaction.completed")
                .withArgument("x-dead-letter-exchange", dlx)
                .withArgument("x-dead-letter-routing-key", "transaction.completed.dlq")
                .build();
    }

    @Bean
    public Queue vcbTransactionFailedQueue() {
        return QueueBuilder.durable("vcb.transaction.failed")
                .withArgument("x-dead-letter-exchange", dlx)
                .withArgument("x-dead-letter-routing-key", "transaction.failed.dlq")
                .build();
    }

    @Bean
    public Queue vcbTransactionDlq() {
        return QueueBuilder.durable("vcb.transaction.dlq").build();
    }

    @Bean
    public Binding completedBinding(Queue vcbTransactionCompletedQueue, TopicExchange vcbTransactionsExchange) {
        return BindingBuilder.bind(vcbTransactionCompletedQueue).to(vcbTransactionsExchange).with("transaction.completed");
    }

    @Bean
    public Binding failedBinding(Queue vcbTransactionFailedQueue, TopicExchange vcbTransactionsExchange) {
        return BindingBuilder.bind(vcbTransactionFailedQueue).to(vcbTransactionsExchange).with("transaction.failed");
    }

    @Bean
    public Binding dlqBinding(Queue vcbTransactionDlq, TopicExchange vcbTransactionsDlx) {
        return BindingBuilder.bind(vcbTransactionDlq).to(vcbTransactionsDlx).with("#");
    }
}
