package com.vinicius.vanguarda.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinicius.vanguarda.application.usecase.GetTransactionUseCase;
import com.vinicius.vanguarda.application.usecase.TransferMoneyUseCase;
import com.vinicius.vanguarda.domain.exception.MissingIdempotencyKeyException;
import com.vinicius.vanguarda.domain.exception.TransactionInProgressException;
import com.vinicius.vanguarda.domain.model.enums.Currency;
import com.vinicius.vanguarda.domain.model.enums.TransactionStatus;
import com.vinicius.vanguarda.infrastructure.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@Import(GlobalExceptionHandler.class)
class TransactionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean TransferMoneyUseCase transferMoneyUseCase;
    @MockBean GetTransactionUseCase getTransactionUseCase;

    @Test
    void shouldReturn201OnSuccessfulTransfer() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();

        TransferMoneyUseCase.Output output = new TransferMoneyUseCase.Output(
                txId, idempotencyKey, sourceId, targetId,
                new BigDecimal("100.00"), Currency.BRL, TransactionStatus.COMPLETED,
                new BigDecimal("400.00"), new BigDecimal("300.00"), Instant.now(), false);

        when(transferMoneyUseCase.execute(any())).thenReturn(output);

        String body = objectMapper.writeValueAsString(Map.of(
                "sourceAccountId", sourceId,
                "targetAccountId", targetId,
                "amount", "100.00"
        ));

        mockMvc.perform(post("/v1/transactions")
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value(txId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.sourceBalanceAfter").value(400.00));
    }

    @Test
    void shouldReturn200WithReplayHeaderWhenReplayed() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        UUID txId = UUID.randomUUID();

        TransferMoneyUseCase.Output output = new TransferMoneyUseCase.Output(
                txId, idempotencyKey, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("100.00"), Currency.BRL, TransactionStatus.COMPLETED,
                new BigDecimal("400.00"), new BigDecimal("300.00"), Instant.now(), true);

        when(transferMoneyUseCase.execute(any())).thenReturn(output);

        String body = objectMapper.writeValueAsString(Map.of(
                "sourceAccountId", UUID.randomUUID(),
                "targetAccountId", UUID.randomUUID(),
                "amount", "100.00"
        ));

        mockMvc.perform(post("/v1/transactions")
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Idempotency-Replayed", "true"));
    }

    @Test
    void shouldReturn400WhenIdempotencyKeyMissing() throws Exception {
        when(transferMoneyUseCase.execute(any())).thenThrow(new MissingIdempotencyKeyException());

        String body = objectMapper.writeValueAsString(Map.of(
                "sourceAccountId", UUID.randomUUID(),
                "targetAccountId", UUID.randomUUID(),
                "amount", "100.00"
        ));

        mockMvc.perform(post("/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_IDEMPOTENCY_KEY"));
    }

    @Test
    void shouldReturn409WhenTransactionInProgress() throws Exception {
        String key = UUID.randomUUID().toString();
        when(transferMoneyUseCase.execute(any())).thenThrow(new TransactionInProgressException(key));

        String body = objectMapper.writeValueAsString(Map.of(
                "sourceAccountId", UUID.randomUUID(),
                "targetAccountId", UUID.randomUUID(),
                "amount", "100.00"
        ));

        mockMvc.perform(post("/v1/transactions")
                        .header("X-Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRANSACTION_IN_PROGRESS"));
    }

    @Test
    void shouldReturn400OnInvalidPayload() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "sourceAccountId", UUID.randomUUID(),
                "targetAccountId", UUID.randomUUID(),
                "amount", "-50.00"
        ));

        mockMvc.perform(post("/v1/transactions")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
}
