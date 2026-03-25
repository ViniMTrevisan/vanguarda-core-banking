package com.vinicius.vanguarda.domain.model;

import com.vinicius.vanguarda.domain.model.enums.Currency;
import com.vinicius.vanguarda.domain.model.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class Transaction {

    private final UUID id;
    private final String idempotencyKey;
    private final UUID sourceAccountId;
    private final UUID targetAccountId;
    private final BigDecimal amount;
    private final Currency currency;
    private TransactionStatus status;
    private final String description;
    private final Map<String, Object> metadata;
    private final Instant processedAt;

    private Transaction(UUID id, String idempotencyKey, UUID sourceAccountId, UUID targetAccountId,
                        BigDecimal amount, Currency currency, TransactionStatus status,
                        String description, Map<String, Object> metadata, Instant processedAt) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.description = description;
        this.metadata = metadata;
        this.processedAt = processedAt;
    }

    public static Transaction create(String idempotencyKey, UUID sourceAccountId, UUID targetAccountId,
                                     BigDecimal amount, Currency currency,
                                     String description, Map<String, Object> metadata) {
        return new Transaction(UUID.randomUUID(), idempotencyKey, sourceAccountId, targetAccountId,
                amount, currency, TransactionStatus.PROCESSING, description, metadata, Instant.now());
    }

    public static Transaction restore(UUID id, String idempotencyKey, UUID sourceAccountId, UUID targetAccountId,
                                       BigDecimal amount, Currency currency, TransactionStatus status,
                                       String description, Map<String, Object> metadata, Instant processedAt) {
        return new Transaction(id, idempotencyKey, sourceAccountId, targetAccountId,
                amount, currency, status, description, metadata, processedAt);
    }

    public void complete() {
        this.status = TransactionStatus.COMPLETED;
    }

    public void fail() {
        this.status = TransactionStatus.FAILED;
    }

    public UUID getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public UUID getSourceAccountId() { return sourceAccountId; }
    public UUID getTargetAccountId() { return targetAccountId; }
    public BigDecimal getAmount() { return amount; }
    public Currency getCurrency() { return currency; }
    public TransactionStatus getStatus() { return status; }
    public String getDescription() { return description; }
    public Map<String, Object> getMetadata() { return metadata; }
    public Instant getProcessedAt() { return processedAt; }
}
