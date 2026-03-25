package com.vinicius.vanguarda.infrastructure.web.dto.response;

import com.vinicius.vanguarda.domain.model.enums.AccountStatus;
import com.vinicius.vanguarda.domain.model.enums.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID accountId,
        String ownerId,
        String ownerName,
        Currency currency,
        BigDecimal balance,
        AccountStatus status,
        Instant createdAt
) {}
