package com.vinicius.vanguarda.domain.repository;

import com.vinicius.vanguarda.domain.model.LedgerEntry;
import com.vinicius.vanguarda.domain.model.enums.EntryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface LedgerEntryRepository {

    LedgerEntry save(LedgerEntry entry);

    Page<LedgerEntry> findByAccountId(UUID accountId, Pageable pageable);

    Page<LedgerEntry> findByAccountIdAndType(UUID accountId, EntryType type, Pageable pageable);

    Page<LedgerEntry> findByAccountIdAndDateRange(UUID accountId, Instant from, Instant to, Pageable pageable);

    Page<LedgerEntry> findByAccountIdAndTypeAndDateRange(UUID accountId, EntryType type,
                                                          Instant from, Instant to, Pageable pageable);
}
