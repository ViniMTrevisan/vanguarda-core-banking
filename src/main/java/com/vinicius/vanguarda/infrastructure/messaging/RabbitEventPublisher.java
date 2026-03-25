package com.vinicius.vanguarda.infrastructure.messaging;

import com.vinicius.vanguarda.domain.service.EventPublisher;
import com.vinicius.vanguarda.infrastructure.metrics.TransactionMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final TransactionMetrics metrics;
    private final String exchange;

    public RabbitEventPublisher(RabbitTemplate rabbitTemplate,
                                 TransactionMetrics metrics,
                                 @Value("${vcb.rabbitmq.exchange:vcb.transactions}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.metrics = metrics;
        this.exchange = exchange;
    }

    @Override
    public void publish(Object event) {
        try {
            String routingKey = resolveRoutingKey(event);
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.debug("Published event {} to exchange={} routingKey={}", event.getClass().getSimpleName(), exchange, routingKey);
        } catch (Exception e) {
            metrics.recordRabbitMQPublishFailure();
            log.error("Failed to publish event {} to RabbitMQ: {}", event.getClass().getSimpleName(), e.getMessage());
            // Never rethrow — RabbitMQ failure must not roll back committed transactions
        }
    }

    private String resolveRoutingKey(Object event) {
        String className = event.getClass().getSimpleName();
        return switch (className) {
            case "TransactionCompletedEvent" -> "transaction.completed";
            case "TransactionFailedEvent" -> "transaction.failed";
            default -> "transaction.event";
        };
    }
}
