package com.vinicius.vanguarda.infrastructure.web.dto.response;

import com.vinicius.vanguarda.domain.model.enums.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BalanceResponse(
        UUID accountId,
        BigDecimal balance,
        Currency currency,
        Instant snapshotAt
) {}
