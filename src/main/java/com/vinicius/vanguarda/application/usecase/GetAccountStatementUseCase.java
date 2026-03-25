package com.vinicius.vanguarda.application.usecase;

import com.vinicius.vanguarda.domain.exception.AccountNotFoundException;
import com.vinicius.vanguarda.domain.model.LedgerEntry;
import com.vinicius.vanguarda.domain.model.enums.EntryType;
import com.vinicius.vanguarda.domain.repository.AccountRepository;
import com.vinicius.vanguarda.domain.repository.LedgerEntryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class GetAccountStatementUseCase {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public GetAccountStatementUseCase(AccountRepository accountRepository,
                                      LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional(readOnly = true)
    public Page<Output> execute(UUID accountId, EntryType type, Instant from, Instant to, Pageable pageable) {
        accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        Page<LedgerEntry> entries;

        if (type != null && from != null && to != null) {
            entries = ledgerEntryRepository.findByAccountIdAndTypeAndDateRange(accountId, type, from, to, pageable);
        } else if (type != null) {
            entries = ledgerEntryRepository.findByAccountIdAndType(accountId, type, pageable);
        } else if (from != null && to != null) {
            entries = ledgerEntryRepository.findByAccountIdAndDateRange(accountId, from, to, pageable);
        } else {
            entries = ledgerEntryRepository.findByAccountId(accountId, pageable);
        }

        return entries.map(this::toOutput);
    }

    private Output toOutput(LedgerEntry entry) {
        return new Output(
                entry.getId(),
                entry.getTransactionId(),
                entry.getAccountId(),
                entry.getType(),
                entry.getAmount(),
                entry.getBalanceBefore(),
                entry.getBalanceAfter(),
                entry.getCreatedAt()
        );
    }

    public record Output(
            UUID entryId,
            UUID transactionId,
            UUID accountId,
            EntryType type,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            Instant createdAt
    ) {}
}
