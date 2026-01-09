package com.example.enterprise.payment.domain.model;

import java.math.BigDecimal;

/**
 * Value Object representing an amount in payment context.
 */
public record PaymentAmount1(BigDecimal value, String currency) {
    public PaymentAmount1 {
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

    public PaymentAmount1 add(PaymentAmount1 other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add amounts with different currencies");
        }
        return new PaymentAmount1(this.value.add(other.value), this.currency);
    }

    public static PaymentAmount1 zero(String currency) {
        return new PaymentAmount1(BigDecimal.ZERO, currency);
    }
}
