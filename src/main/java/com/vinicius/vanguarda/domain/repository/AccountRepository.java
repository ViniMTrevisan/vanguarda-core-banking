package com.vinicius.vanguarda.domain.repository;

import com.vinicius.vanguarda.domain.model.Account;
import com.vinicius.vanguarda.domain.model.enums.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {

    Optional<Account> findById(UUID id);

    Optional<Account> findByIdForUpdate(UUID id);

    Page<Account> findByOwnerId(String ownerId, Pageable pageable);

    Page<Account> findByStatus(AccountStatus status, Pageable pageable);

    Page<Account> findByOwnerIdAndStatus(String ownerId, AccountStatus status, Pageable pageable);

    Account save(Account account);

    org.springframework.data.domain.Page<Account> findAll(org.springframework.data.domain.Pageable pageable);
}
