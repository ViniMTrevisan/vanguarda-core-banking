package com.vinicius.vanguarda.application.usecase;

import com.vinicius.vanguarda.domain.exception.AccountNotFoundException;
import com.vinicius.vanguarda.domain.model.Account;
import com.vinicius.vanguarda.domain.model.enums.AccountStatus;
import com.vinicius.vanguarda.domain.model.enums.Currency;
import com.vinicius.vanguarda.domain.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class GetAccountUseCase {

    private final AccountRepository accountRepository;

    public GetAccountUseCase(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public Output execute(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        return toOutput(account);
    }

    private Output toOutput(Account account) {
        return new Output(
                account.getId(),
                account.getOwnerId(),
                account.getOwnerName(),
                account.getCurrency(),
                account.getBalance(),
                account.getStatus(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }

    public record Output(
            UUID accountId,
            String ownerId,
            String ownerName,
            Currency currency,
            BigDecimal balance,
            AccountStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
