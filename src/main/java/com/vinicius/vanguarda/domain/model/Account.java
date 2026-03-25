package com.vinicius.vanguarda.domain.model;

import com.vinicius.vanguarda.domain.model.enums.AccountStatus;
import com.vinicius.vanguarda.domain.model.enums.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Account {

    private final UUID id;
    private final String ownerId;
    private final String ownerName;
    private final Currency currency;
    private BigDecimal balance;
    private AccountStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private final Long version;

    private Account(UUID id, String ownerId, String ownerName, Currency currency,
                    BigDecimal balance, AccountStatus status, Instant createdAt, Instant updatedAt, Long version) {
        this.id = id;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.currency = currency;
        this.balance = balance;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static Account create(String ownerId, String ownerName, Currency currency, BigDecimal initialBalance) {
        if (initialBalance == null || initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial balance must be >= 0");
        }
        Instant now = Instant.now();
        return new Account(UUID.randomUUID(), ownerId, ownerName, currency,
                initialBalance, AccountStatus.ACTIVE, now, now, null);
    }

    public static Account restore(UUID id, String ownerId, String ownerName, Currency currency,
                                   BigDecimal balance, AccountStatus status, Instant createdAt, Instant updatedAt,
                                   Long version) {
        return new Account(id, ownerId, ownerName, currency, balance, status, createdAt, updatedAt, version);
    }

    public void debit(Money money) {
        if (!canSend()) {
            throw new IllegalStateException("Account " + id + " cannot send funds (status: " + status + ")");
        }
        Money current = Money.of(this.balance, this.currency);
        Money toDebit = money;
        if (!current.isGreaterThanOrEqual(toDebit)) {
            throw new IllegalStateException("Insufficient balance");
        }
        this.balance = current.subtract(toDebit).getAmount();
        this.updatedAt = Instant.now();
    }

    public void credit(Money money) {
        if (!canReceive()) {
            throw new IllegalStateException("Account " + id + " cannot receive funds (status: " + status + ")");
        }
        Money current = Money.of(this.balance, this.currency);
        this.balance = current.add(money).getAmount();
        this.updatedAt = Instant.now();
    }

    public void freeze() {
        if (this.status == AccountStatus.CLOSED) {
            throw new IllegalStateException("Cannot freeze a closed account");
        }
        this.status = AccountStatus.FROZEN;
        this.updatedAt = Instant.now();
    }

    public void close() {
        this.status = AccountStatus.CLOSED;
        this.updatedAt = Instant.now();
    }

    public boolean canSend() {
        return this.status == AccountStatus.ACTIVE;
    }

    public boolean canReceive() {
        return this.status == AccountStatus.ACTIVE || this.status == AccountStatus.FROZEN;
    }

    public UUID getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public String getOwnerName() { return ownerName; }
    public Currency getCurrency() { return currency; }
    public BigDecimal getBalance() { return balance; }
    public AccountStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
