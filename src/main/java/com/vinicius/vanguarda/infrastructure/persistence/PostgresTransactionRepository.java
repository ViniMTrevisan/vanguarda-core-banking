package com.vinicius.vanguarda.infrastructure.persistence;

import com.vinicius.vanguarda.domain.model.Transaction;
import com.vinicius.vanguarda.domain.repository.TransactionRepository;
import com.vinicius.vanguarda.infrastructure.persistence.entity.TransactionEntity;
import com.vinicius.vanguarda.infrastructure.persistence.jpa.JpaTransactionRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class PostgresTransactionRepository implements TransactionRepository {

    private final JpaTransactionRepository jpa;

    public PostgresTransactionRepository(JpaTransactionRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        return jpa.findById(id).map(TransactionEntity::toDomain);
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity entity = jpa.save(TransactionEntity.fromDomain(transaction));
        return entity.toDomain();
    }

    @Override
    public Transaction update(Transaction transaction) {
        return save(transaction);
    }
}
