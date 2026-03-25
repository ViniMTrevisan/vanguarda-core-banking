package com.vinicius.vanguarda.domain.model;

import com.vinicius.vanguarda.domain.model.enums.Currency;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class MoneyTest {

    @Test
    void shouldCreateMoneyWithScale2() {
        Money money = Money.of(new BigDecimal("100.1"), Currency.BRL);
        assertThat(money.getAmount()).isEqualByComparingTo("100.10");
        assertThat(money.getCurrency()).isEqualTo(Currency.BRL);
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("-1.00"), Currency.BRL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
    }

    @Test
    void shouldRejectNullAmount() {
        assertThatThrownBy(() -> Money.of(null, Currency.BRL))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldAddMoneyWithSameCurrency() {
        Money a = Money.of(new BigDecimal("100.00"), Currency.BRL);
        Money b = Money.of(new BigDecimal("50.00"), Currency.BRL);
        assertThat(a.add(b).getAmount()).isEqualByComparingTo("150.00");
    }

    @Test
    void shouldSubtractMoneyWithSameCurrency() {
        Money a = Money.of(new BigDecimal("100.00"), Currency.BRL);
        Money b = Money.of(new BigDecimal("30.00"), Currency.BRL);
        assertThat(a.subtract(b).getAmount()).isEqualByComparingTo("70.00");
    }

    @Test
    void shouldRejectSubtractionResultingInNegative() {
        Money a = Money.of(new BigDecimal("10.00"), Currency.BRL);
        Money b = Money.of(new BigDecimal("20.00"), Currency.BRL);
        assertThatThrownBy(() -> a.subtract(b))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectOperationsOnDifferentCurrencies() {
        Money brl = Money.of(new BigDecimal("100.00"), Currency.BRL);
        Money usd = Money.of(new BigDecimal("100.00"), Currency.USD);
        assertThatThrownBy(() -> brl.add(usd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currencies");
    }

    @Test
    void shouldCompareGreaterThanOrEqual() {
        Money hundred = Money.of(new BigDecimal("100.00"), Currency.BRL);
        Money fifty = Money.of(new BigDecimal("50.00"), Currency.BRL);
        assertThat(hundred.isGreaterThanOrEqual(fifty)).isTrue();
        assertThat(hundred.isGreaterThanOrEqual(hundred)).isTrue();
        assertThat(fifty.isGreaterThanOrEqual(hundred)).isFalse();
    }
}
