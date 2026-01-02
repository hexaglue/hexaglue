package com.ecommerce.domain.order;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * Money value object representing an amount with currency.
 */
public record Money(BigDecimal amount, Currency currency) {

    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, Currency.getInstance(currencyCode));
    }

    public static Money euros(BigDecimal amount) {
        return of(amount, "EUR");
    }

    public static Money dollars(BigDecimal amount) {
        return of(amount, "USD");
    }

    public Money add(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add different currencies");
        }
        return new Money(amount.add(other.amount), currency);
    }

    public Money multiply(int quantity) {
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)), currency);
    }
}
