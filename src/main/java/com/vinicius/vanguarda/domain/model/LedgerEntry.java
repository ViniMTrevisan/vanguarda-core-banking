package com.vinicius.vanguarda.domain.model;

import com.vinicius.vanguarda.domain.model.enums.EntryType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class LedgerEntry {

    private final UUID id;
    private final UUID transactionId;
    private final UUID accountId;
    private final EntryType type;
    private final BigDecimal amount;
    private final BigDecimal balanceBefore;
    private final BigDecimal balanceAfter;
    private final Instant createdAt;

    public LedgerEntry(UUID transactionId, UUID accountId, EntryType type,
                       BigDecimal amount, BigDecimal balanceBefore, BigDecimal balanceAfter) {
        this.id = UUID.randomUUID();
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.createdAt = Instant.now();
    }

    public LedgerEntry(UUID id, UUID transactionId, UUID accountId, EntryType type,
                       BigDecimal amount, BigDecimal balanceBefore, BigDecimal balanceAfter, Instant createdAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getTransactionId() { return transactionId; }
    public UUID getAccountId() { return accountId; }
    public EntryType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceBefore() { return balanceBefore; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public Instant getCreatedAt() { return createdAt; }
}
