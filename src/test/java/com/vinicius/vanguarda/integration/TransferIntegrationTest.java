package com.vinicius.vanguarda.integration;

import com.vinicius.vanguarda.application.usecase.CreateAccountUseCase;
import com.vinicius.vanguarda.application.usecase.GetAccountStatementUseCase;
import com.vinicius.vanguarda.application.usecase.TransferMoneyUseCase;
import com.vinicius.vanguarda.domain.exception.*;
import com.vinicius.vanguarda.domain.model.enums.Currency;
import com.vinicius.vanguarda.domain.model.enums.EntryType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class TransferIntegrationTest extends AbstractIntegrationTest {

    @Autowired CreateAccountUseCase createAccountUseCase;
    @Autowired TransferMoneyUseCase transferMoneyUseCase;
    @Autowired GetAccountStatementUseCase getAccountStatementUseCase;

    @Test
    void tc08_shouldCreateExactlyOneDebitAndOneCreditOnSuccessfulTransfer() {
        // Create accounts
        var sourceOutput = createAccountUseCase.execute(new CreateAccountUseCase.Input(
                "owner1", "Alice", Currency.BRL, new BigDecimal("1000.00")));
        var targetOutput = createAccountUseCase.execute(new CreateAccountUseCase.Input(
                "owner2", "Bob", Currency.BRL, new BigDecimal("500.00")));

        String key = UUID.randomUUID().toString();
        TransferMoneyUseCase.Output txOutput = transferMoneyUseCase.execute(new TransferMoneyUseCase.Input(
                key, sourceOutput.accountId(), targetOutput.accountId(),
                new BigDecimal("250.00"), "Test transfer", null));

        assertThat(txOutput.sourceBalanceAfter()).isEqualByComparingTo("750.00");
        assertThat(txOutput.targetBalanceAfter()).isEqualByComparingTo("750.00");

        // TC-08: Verify exactly 1 DEBIT + 1 CREDIT with correct balance snapshots
        Page<GetAccountStatementUseCase.Output> sourceEntries = getAccountStatementUseCase
                .execute(sourceOutput.accountId(), null, null, null, PageRequest.of(0, 10));
        Page<GetAccountStatementUseCase.Output> targetEntries = getAccountStatementUseCase
                .execute(targetOutput.accountId(), null, null, null, PageRequest.of(0, 10));

        assertThat(sourceEntries.getTotalElements()).isEqualTo(1);
        assertThat(targetEntries.getTotalElements()).isEqualTo(1);

        var debit = sourceEntries.getContent().get(0);
        assertThat(debit.type()).isEqualTo(EntryType.DEBIT);
        assertThat(debit.amount()).isEqualByComparingTo("250.00");
        assertThat(debit.balanceBefore()).isEqualByComparingTo("1000.00");
        assertThat(debit.balanceAfter()).isEqualByComparingTo("750.00");

        var credit = targetEntries.getContent().get(0);
        assertThat(credit.type()).isEqualTo(EntryType.CREDIT);
        assertThat(credit.amount()).isEqualByComparingTo("250.00");
        assertThat(credit.balanceBefore()).isEqualByComparingTo("500.00");
        assertThat(credit.balanceAfter()).isEqualByComparingTo("750.00");
    }

    @Test
    void tc05_shouldReturn422AndLeaveLedgerUnchangedOnInsufficientBalance() {
        var sourceOutput = createAccountUseCase.execute(new CreateAccountUseCase.Input(
                "owner3", "Charlie", Currency.BRL, new BigDecimal("50.00")));
        var targetOutput = createAccountUseCase.execute(new CreateAccountUseCase.Input(
                "owner4", "Dave", Currency.BRL, new BigDecimal("100.00")));

        assertThatThrownBy(() -> transferMoneyUseCase.execute(new TransferMoneyUseCase.Input(
                UUID.randomUUID().toString(),
                sourceOutput.accountId(), targetOutput.accountId(),
                new BigDecimal("100.00"), null, null)))
                .isInstanceOf(InsufficientBalanceException.class);

        // Ledger must be unchanged
        Page<GetAccountStatementUseCase.Output> entries = getAccountStatementUseCase
                .execute(sourceOutput.accountId(), null, null, null, PageRequest.of(0, 10));
        assertThat(entries.getTotalElements()).isZero();
    }

    @Test
    void tc07_shouldReturn422OnCurrencyMismatch() {
        var brlAccount = createAccountUseCase.execute(new CreateAccountUseCase.Input(
                "owner5", "Eve", Currency.BRL, new BigDecimal("500.00")));
        var usdAccount = createAccountUseCase.execute(new CreateAccountUseCase.Input(
                "owner6", "Frank", Currency.USD, new BigDecimal("500.00")));

        assertThatThrownBy(() -> transferMoneyUseCase.execute(new TransferMoneyUseCase.Input(
                UUID.randomUUID().toString(),
                brlAccount.accountId(), usdAccount.accountId(),
                new BigDecimal("100.00"), null, null)))
                .isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    void shouldReturn422WhenSameAccountTransfer() {
        var account = createAccountUseCase.execute(new CreateAccountUseCase.Input(
                "owner7", "Grace", Currency.BRL, new BigDecimal("500.00")));

        assertThatThrownBy(() -> transferMoneyUseCase.execute(new TransferMoneyUseCase.Input(
                UUID.randomUUID().toString(),
                account.accountId(), account.accountId(),
                new BigDecimal("100.00"), null, null)));
        // The DB constraint will enforce this; domain validation catches source==target
    }
}
