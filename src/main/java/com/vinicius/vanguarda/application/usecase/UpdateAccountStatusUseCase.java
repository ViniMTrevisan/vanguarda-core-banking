package com.vinicius.vanguarda.application.usecase;

import com.vinicius.vanguarda.domain.exception.AccountNotFoundException;
import com.vinicius.vanguarda.domain.model.Account;
import com.vinicius.vanguarda.domain.model.enums.AccountStatus;
import com.vinicius.vanguarda.domain.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UpdateAccountStatusUseCase {

    private final AccountRepository accountRepository;

    public UpdateAccountStatusUseCase(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public void execute(UUID accountId, AccountStatus newStatus) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        switch (newStatus) {
            case FROZEN -> account.freeze();
            case CLOSED -> account.close();
            case ACTIVE -> throw new IllegalArgumentException(
                    "Cannot transition account to ACTIVE via status update. CLOSED accounts cannot be reopened.");
        }

        accountRepository.save(account);
    }
}
