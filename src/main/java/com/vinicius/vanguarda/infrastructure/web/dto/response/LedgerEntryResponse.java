package com.vinicius.vanguarda.infrastructure.web.dto.response;

import com.vinicius.vanguarda.domain.model.enums.EntryType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntryResponse(
        UUID entryId,
        UUID transactionId,
        UUID accountId,
        EntryType type,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        Instant createdAt
) {}
