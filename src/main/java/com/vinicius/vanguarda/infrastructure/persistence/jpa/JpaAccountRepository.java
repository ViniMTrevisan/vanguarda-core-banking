package com.vinicius.vanguarda.infrastructure.persistence.jpa;

import com.vinicius.vanguarda.domain.model.enums.AccountStatus;
import com.vinicius.vanguarda.infrastructure.persistence.entity.AccountEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JpaAccountRepository extends JpaRepository<AccountEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AccountEntity a WHERE a.id = :id")
    Optional<AccountEntity> findByIdForUpdate(@Param("id") UUID id);

    Page<AccountEntity> findByOwnerId(String ownerId, Pageable pageable);

    Page<AccountEntity> findByStatus(AccountStatus status, Pageable pageable);

    Page<AccountEntity> findByOwnerIdAndStatus(String ownerId, AccountStatus status, Pageable pageable);
}
