package com.vinicius.vanguarda.infrastructure.web.dto.response;

import com.vinicius.vanguarda.domain.model.enums.Currency;
import com.vinicius.vanguarda.domain.model.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID transactionId,
        String idempotencyKey,
        UUID sourceAccountId,
        UUID targetAccountId,
        BigDecimal amount,
        Currency currency,
        TransactionStatus status,
        BigDecimal sourceBalanceAfter,
        BigDecimal targetBalanceAfter,
        Instant processedAt
) {}
