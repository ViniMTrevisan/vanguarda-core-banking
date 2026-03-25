package com.vinicius.vanguarda.application.usecase;

import com.vinicius.vanguarda.domain.model.Account;
import com.vinicius.vanguarda.domain.model.enums.AccountStatus;
import com.vinicius.vanguarda.domain.model.enums.Currency;
import com.vinicius.vanguarda.domain.repository.AccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class ListAccountsUseCase {

    private final AccountRepository accountRepository;

    public ListAccountsUseCase(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public Page<Output> execute(String ownerId, AccountStatus status, Pageable pageable) {
        Page<Account> accounts;

        if (ownerId != null && status != null) {
            accounts = accountRepository.findByOwnerIdAndStatus(ownerId, status, pageable);
        } else if (ownerId != null) {
            accounts = accountRepository.findByOwnerId(ownerId, pageable);
        } else if (status != null) {
            accounts = accountRepository.findByStatus(status, pageable);
        } else {
            accounts = accountRepository.findAll(pageable);
        }

        return accounts.map(this::toOutput);
    }

    private Output toOutput(Account account) {
        return new Output(
                account.getId(),
                account.getOwnerId(),
                account.getOwnerName(),
                account.getCurrency(),
                account.getBalance(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }

    public record Output(
            UUID accountId,
            String ownerId,
            String ownerName,
            Currency currency,
            BigDecimal balance,
            AccountStatus status,
            Instant createdAt
    ) {}
}
