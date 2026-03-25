package com.vinicius.vanguarda.application.usecase;

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
public class CreateAccountUseCase {

    private final AccountRepository accountRepository;

    public CreateAccountUseCase(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public Output execute(Input input) {
        Account account = Account.create(input.ownerId(), input.ownerName(), input.currency(), input.initialBalance());
        Account saved = accountRepository.save(account);
        return toOutput(saved);
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

    public record Input(
            String ownerId,
            String ownerName,
            Currency currency,
            BigDecimal initialBalance
    ) {}

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
