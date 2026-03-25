package com.vinicius.vanguarda.domain.repository;

import com.vinicius.vanguarda.domain.model.Transaction;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository {

    Optional<Transaction> findById(UUID id);

    Transaction save(Transaction transaction);

    Transaction update(Transaction transaction);
}
