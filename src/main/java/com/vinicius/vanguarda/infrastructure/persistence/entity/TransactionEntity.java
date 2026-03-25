package com.vinicius.vanguarda.infrastructure.persistence.entity;

import com.vinicius.vanguarda.domain.model.Transaction;
import com.vinicius.vanguarda.domain.model.enums.Currency;
import com.vinicius.vanguarda.domain.model.enums.TransactionStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class TransactionEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "source_account_id", nullable = false, columnDefinition = "uuid")
    private UUID sourceAccountId;

    @Column(name = "target_account_id", nullable = false, columnDefinition = "uuid")
    private UUID targetAccountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(length = 255)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected TransactionEntity() {}

    @SuppressWarnings("unchecked")
    public Transaction toDomain() {
        Map<String, Object> metadataMap = null;
        if (metadata != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                metadataMap = mapper.readValue(metadata, Map.class);
            } catch (Exception e) {
                metadataMap = Map.of();
            }
        }
        return Transaction.restore(id, idempotencyKey, sourceAccountId, targetAccountId,
                amount, currency, status, description, metadataMap, processedAt);
    }

    public static TransactionEntity fromDomain(Transaction transaction) {
        TransactionEntity entity = new TransactionEntity();
        entity.id = transaction.getId();
        entity.idempotencyKey = transaction.getIdempotencyKey();
        entity.sourceAccountId = transaction.getSourceAccountId();
        entity.targetAccountId = transaction.getTargetAccountId();
        entity.amount = transaction.getAmount();
        entity.currency = transaction.getCurrency();
        entity.status = transaction.getStatus();
        entity.description = transaction.getDescription();
        if (transaction.getMetadata() != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                entity.metadata = mapper.writeValueAsString(transaction.getMetadata());
            } catch (Exception e) {
                entity.metadata = "{}";
            }
        }
        entity.processedAt = transaction.getProcessedAt();
        return entity;
    }

    public UUID getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public UUID getSourceAccountId() { return sourceAccountId; }
    public UUID getTargetAccountId() { return targetAccountId; }
    public BigDecimal getAmount() { return amount; }
    public Currency getCurrency() { return currency; }
    public TransactionStatus getStatus() { return status; }
    public String getDescription() { return description; }
    public String getMetadata() { return metadata; }
    public Instant getProcessedAt() { return processedAt; }

    public void setStatus(TransactionStatus status) { this.status = status; }
}
