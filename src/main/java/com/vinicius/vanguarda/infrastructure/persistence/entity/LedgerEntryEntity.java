package com.vinicius.vanguarda.infrastructure.persistence.entity;

import com.vinicius.vanguarda.domain.model.LedgerEntry;
import com.vinicius.vanguarda.domain.model.enums.EntryType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntryEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "transaction_id", nullable = false, columnDefinition = "uuid")
    private UUID transactionId;

    @Column(name = "account_id", nullable = false, columnDefinition = "uuid")
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EntryType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_before", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LedgerEntryEntity() {}

    public LedgerEntry toDomain() {
        return new LedgerEntry(id, transactionId, accountId, type, amount, balanceBefore, balanceAfter, createdAt);
    }

    public static LedgerEntryEntity fromDomain(LedgerEntry entry) {
        LedgerEntryEntity entity = new LedgerEntryEntity();
        entity.id = entry.getId();
        entity.transactionId = entry.getTransactionId();
        entity.accountId = entry.getAccountId();
        entity.type = entry.getType();
        entity.amount = entry.getAmount();
        entity.balanceBefore = entry.getBalanceBefore();
        entity.balanceAfter = entry.getBalanceAfter();
        entity.createdAt = entry.getCreatedAt();
        return entity;
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
