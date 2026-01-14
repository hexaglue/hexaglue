package com.example.domain;

import java.math.BigDecimal;

/**
 * Value object representing a monetary amount.
 */
public record Money(BigDecimal amount, String currency) {

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    public static Money euros(BigDecimal amount) {
        return new Money(amount, "EUR");
    }

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add different currencies");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money multiply(int quantity) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)), this.currency);
    }
}
