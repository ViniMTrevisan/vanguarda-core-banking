package com.vinicius.vanguarda.domain.model;

import com.vinicius.vanguarda.domain.model.enums.AccountStatus;
import com.vinicius.vanguarda.domain.model.enums.Currency;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class AccountTest {

    @Test
    void shouldCreateActiveAccountWithInitialBalance() {
        Account account = Account.create("owner1", "Alice", Currency.BRL, new BigDecimal("1000.00"));
        assertThat(account.getId()).isNotNull();
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getBalance()).isEqualByComparingTo("1000.00");
        assertThat(account.getCurrency()).isEqualTo(Currency.BRL);
        assertThat(account.canSend()).isTrue();
        assertThat(account.canReceive()).isTrue();
    }

    @Test
    void shouldRejectNegativeInitialBalance() {
        assertThatThrownBy(() -> Account.create("owner1", "Alice", Currency.BRL, new BigDecimal("-1.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldDebitActiveAccount() {
        Account account = Account.create("o1", "Alice", Currency.BRL, new BigDecimal("1000.00"));
        account.debit(Money.of(new BigDecimal("300.00"), Currency.BRL));
        assertThat(account.getBalance()).isEqualByComparingTo("700.00");
    }

    @Test
    void shouldRejectDebitWithInsufficientBalance() {
        Account account = Account.create("o1", "Alice", Currency.BRL, new BigDecimal("100.00"));
        assertThatThrownBy(() -> account.debit(Money.of(new BigDecimal("200.00"), Currency.BRL)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldCreditActiveAccount() {
        Account account = Account.create("o1", "Alice", Currency.BRL, new BigDecimal("500.00"));
        account.credit(Money.of(new BigDecimal("200.00"), Currency.BRL));
        assertThat(account.getBalance()).isEqualByComparingTo("700.00");
    }

    @Test
    void shouldFreezeActiveAccount() {
        Account account = Account.create("o1", "Alice", Currency.BRL, BigDecimal.ZERO);
        account.freeze();
        assertThat(account.getStatus()).isEqualTo(AccountStatus.FROZEN);
        assertThat(account.canSend()).isFalse();
        assertThat(account.canReceive()).isTrue();
    }

    @Test
    void shouldCloseAccount() {
        Account account = Account.create("o1", "Alice", Currency.BRL, BigDecimal.ZERO);
        account.close();
        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
        assertThat(account.canSend()).isFalse();
        assertThat(account.canReceive()).isFalse();
    }

    @Test
    void shouldRejectFreezeOnClosedAccount() {
        Account account = Account.create("o1", "Alice", Currency.BRL, BigDecimal.ZERO);
        account.close();
        assertThatThrownBy(account::freeze)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void shouldRejectDebitOnFrozenAccount() {
        Account account = Account.create("o1", "Alice", Currency.BRL, new BigDecimal("500.00"));
        account.freeze();
        assertThatThrownBy(() -> account.debit(Money.of(new BigDecimal("100.00"), Currency.BRL)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldRejectCreditOnClosedAccount() {
        Account account = Account.create("o1", "Alice", Currency.BRL, new BigDecimal("500.00"));
        account.close();
        assertThatThrownBy(() -> account.credit(Money.of(new BigDecimal("100.00"), Currency.BRL)))
                .isInstanceOf(IllegalStateException.class);
    }
}
