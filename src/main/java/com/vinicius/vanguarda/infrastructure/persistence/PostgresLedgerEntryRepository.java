package com.vinicius.vanguarda.infrastructure.persistence;

import com.vinicius.vanguarda.domain.model.LedgerEntry;
import com.vinicius.vanguarda.domain.model.enums.EntryType;
import com.vinicius.vanguarda.domain.repository.LedgerEntryRepository;
import com.vinicius.vanguarda.infrastructure.persistence.entity.LedgerEntryEntity;
import com.vinicius.vanguarda.infrastructure.persistence.jpa.JpaLedgerEntryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public class PostgresLedgerEntryRepository implements LedgerEntryRepository {

    private final JpaLedgerEntryRepository jpa;

    public PostgresLedgerEntryRepository(JpaLedgerEntryRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public LedgerEntry save(LedgerEntry entry) {
        LedgerEntryEntity entity = jpa.save(LedgerEntryEntity.fromDomain(entry));
        return entity.toDomain();
    }

    @Override
    public Page<LedgerEntry> findByAccountId(UUID accountId, Pageable pageable) {
        return jpa.findByAccountId(accountId, pageable).map(LedgerEntryEntity::toDomain);
    }

    @Override
    public Page<LedgerEntry> findByAccountIdAndType(UUID accountId, EntryType type, Pageable pageable) {
        return jpa.findByAccountIdAndType(accountId, type, pageable).map(LedgerEntryEntity::toDomain);
    }

    @Override
    public Page<LedgerEntry> findByAccountIdAndDateRange(UUID accountId, Instant from, Instant to, Pageable pageable) {
        return jpa.findByAccountIdAndCreatedAtBetween(accountId, from, to, pageable).map(LedgerEntryEntity::toDomain);
    }

    @Override
    public Page<LedgerEntry> findByAccountIdAndTypeAndDateRange(UUID accountId, EntryType type,
                                                                 Instant from, Instant to, Pageable pageable) {
        return jpa.findByAccountIdAndTypeAndCreatedAtBetween(accountId, type, from, to, pageable)
                .map(LedgerEntryEntity::toDomain);
    }
}
