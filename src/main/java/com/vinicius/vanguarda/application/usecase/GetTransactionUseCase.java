package com.vinicius.vanguarda.application.usecase;

import com.vinicius.vanguarda.domain.exception.TransactionNotFoundException;
import com.vinicius.vanguarda.domain.model.Transaction;
import com.vinicius.vanguarda.domain.model.enums.Currency;
import com.vinicius.vanguarda.domain.model.enums.TransactionStatus;
import com.vinicius.vanguarda.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class GetTransactionUseCase {

    private final TransactionRepository transactionRepository;

    public GetTransactionUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public Output execute(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
        return toOutput(transaction);
    }

    private Output toOutput(Transaction tx) {
        return new Output(
                tx.getId(),
                tx.getIdempotencyKey(),
                tx.getSourceAccountId(),
                tx.getTargetAccountId(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getStatus(),
                tx.getDescription(),
                tx.getMetadata(),
                tx.getProcessedAt()
        );
    }

    public record Output(
            UUID transactionId,
            String idempotencyKey,
            UUID sourceAccountId,
            UUID targetAccountId,
            BigDecimal amount,
            Currency currency,
            TransactionStatus status,
            String description,
            Map<String, Object> metadata,
            Instant processedAt
    ) {}
}
