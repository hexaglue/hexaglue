package com.example.enterprise.payment.domain.model;

import java.math.BigDecimal;

/**
 * Value Object representing an amount in payment context.
 */
public record PaymentAmount2(BigDecimal value, String currency) {
    public PaymentAmount2 {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency cannot be null or blank");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }

    public PaymentAmount2 add(PaymentAmount2 other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add amounts with different currencies");
        }
        return new PaymentAmount2(this.value.add(other.value), this.currency);
    }

    public static PaymentAmount2 zero(String currency) {
        return new PaymentAmount2(BigDecimal.ZERO, currency);
    }
}
