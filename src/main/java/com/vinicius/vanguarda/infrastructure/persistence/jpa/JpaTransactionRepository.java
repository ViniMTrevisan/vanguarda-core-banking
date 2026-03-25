package com.vinicius.vanguarda.infrastructure.persistence.jpa;

import com.vinicius.vanguarda.infrastructure.persistence.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaTransactionRepository extends JpaRepository<TransactionEntity, UUID> {
}
