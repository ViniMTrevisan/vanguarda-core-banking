package com.vinicius.vanguarda.infrastructure.persistence.jpa;

import com.vinicius.vanguarda.domain.model.enums.EntryType;
import com.vinicius.vanguarda.infrastructure.persistence.entity.LedgerEntryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface JpaLedgerEntryRepository extends JpaRepository<LedgerEntryEntity, UUID> {

    Page<LedgerEntryEntity> findByAccountId(UUID accountId, Pageable pageable);

    Page<LedgerEntryEntity> findByAccountIdAndType(UUID accountId, EntryType type, Pageable pageable);

    Page<LedgerEntryEntity> findByAccountIdAndCreatedAtBetween(UUID accountId, Instant from, Instant to, Pageable pageable);

    Page<LedgerEntryEntity> findByAccountIdAndTypeAndCreatedAtBetween(UUID accountId, EntryType type,
                                                                       Instant from, Instant to, Pageable pageable);
}
