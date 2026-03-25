package com.vinicius.vanguarda.infrastructure.web.controller;

import com.vinicius.vanguarda.application.usecase.GetTransactionUseCase;
import com.vinicius.vanguarda.application.usecase.TransferMoneyUseCase;
import com.vinicius.vanguarda.infrastructure.web.dto.request.CreateTransactionRequest;
import com.vinicius.vanguarda.infrastructure.web.dto.response.TransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/v1/transactions")
@Tag(name = "Transactions", description = "P2P transfer operations")
public class TransactionController {

    private final TransferMoneyUseCase transferMoneyUseCase;
    private final GetTransactionUseCase getTransactionUseCase;

    public TransactionController(TransferMoneyUseCase transferMoneyUseCase,
                                  GetTransactionUseCase getTransactionUseCase) {
        this.transferMoneyUseCase = transferMoneyUseCase;
        this.getTransactionUseCase = getTransactionUseCase;
    }

    @PostMapping
    @Operation(summary = "Process a P2P transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateTransactionRequest request) {

        TransferMoneyUseCase.Input input = new TransferMoneyUseCase.Input(
                idempotencyKey,
                request.sourceAccountId(),
                request.targetAccountId(),
                request.amount(),
                request.description(),
                request.metadata()
        );

        TransferMoneyUseCase.Output output = transferMoneyUseCase.execute(input);
        TransactionResponse response = toResponse(output);

        if (output.replayed()) {
            return ResponseEntity.ok()
                    .header("X-Idempotency-Replayed", "true")
                    .body(response);
        }

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(output.transactionId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<TransactionResponse> getById(@PathVariable UUID transactionId) {
        GetTransactionUseCase.Output output = getTransactionUseCase.execute(transactionId);
        TransactionResponse response = new TransactionResponse(
                output.transactionId(), output.idempotencyKey(),
                output.sourceAccountId(), output.targetAccountId(),
                output.amount(), output.currency(), output.status(),
                null, null, output.processedAt()
        );
        return ResponseEntity.ok(response);
    }

    private TransactionResponse toResponse(TransferMoneyUseCase.Output output) {
        return new TransactionResponse(
                output.transactionId(),
                output.idempotencyKey(),
                output.sourceAccountId(),
                output.targetAccountId(),
                output.amount(),
                output.currency(),
                output.status(),
                output.sourceBalanceAfter(),
                output.targetBalanceAfter(),
                output.processedAt()
        );
    }
}
