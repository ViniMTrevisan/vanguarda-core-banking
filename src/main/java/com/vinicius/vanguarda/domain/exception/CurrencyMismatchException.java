package com.vinicius.vanguarda.domain.exception;

import com.vinicius.vanguarda.domain.model.enums.Currency;

public class CurrencyMismatchException extends RuntimeException {
    public CurrencyMismatchException(Currency source, Currency target) {
        super("Currency mismatch: source=" + source + ", target=" + target);
    }
}
