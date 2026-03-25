package com.vinicius.vanguarda.infrastructure.messaging.event;

import com.vinicius.vanguarda.domain.model.enums.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TransactionCompletedEvent(
        String eventId,
        String eventType,
        UUID transactionId,
        UUID sourceAccountId,
        UUID targetAccountId,
        BigDecimal amount,
        Currency currency,
        Instant processedAt,
        Map<String, Object> metadata
) {}
