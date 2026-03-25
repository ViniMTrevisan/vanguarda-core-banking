package com.vinicius.vanguarda.infrastructure.persistence;

import com.vinicius.vanguarda.domain.model.Account;
import com.vinicius.vanguarda.domain.model.enums.AccountStatus;
import com.vinicius.vanguarda.domain.repository.AccountRepository;
import com.vinicius.vanguarda.infrastructure.persistence.entity.AccountEntity;
import com.vinicius.vanguarda.infrastructure.persistence.jpa.JpaAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class PostgresAccountRepository implements AccountRepository {

    private final JpaAccountRepository jpa;

    public PostgresAccountRepository(JpaAccountRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return jpa.findById(id).map(AccountEntity::toDomain);
    }

    @Override
    public Optional<Account> findByIdForUpdate(UUID id) {
        return jpa.findByIdForUpdate(id).map(AccountEntity::toDomain);
    }

    @Override
    public Page<Account> findByOwnerId(String ownerId, Pageable pageable) {
        return jpa.findByOwnerId(ownerId, pageable).map(AccountEntity::toDomain);
    }

    @Override
    public Page<Account> findByStatus(AccountStatus status, Pageable pageable) {
        return jpa.findByStatus(status, pageable).map(AccountEntity::toDomain);
    }

    @Override
    public Page<Account> findByOwnerIdAndStatus(String ownerId, AccountStatus status, Pageable pageable) {
        return jpa.findByOwnerIdAndStatus(ownerId, status, pageable).map(AccountEntity::toDomain);
    }

    @Override
    public Account save(Account account) {
        AccountEntity entity = jpa.save(AccountEntity.fromDomain(account));
        return entity.toDomain();
    }

    @Override
    public org.springframework.data.domain.Page<Account> findAll(org.springframework.data.domain.Pageable pageable) {
        return jpa.findAll(pageable).map(AccountEntity::toDomain);
    }
}
