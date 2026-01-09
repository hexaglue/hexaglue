package com.example.enterprise.customer.domain.model;

import java.math.BigDecimal;

/**
 * Value Object representing an amount in customer context.
 */
public record CustomerAmount2(BigDecimal value, String currency) {
    public CustomerAmount2 {
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

    public CustomerAmount2 add(CustomerAmount2 other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add amounts with different currencies");
        }
        return new CustomerAmount2(this.value.add(other.value), this.currency);
    }

    public static CustomerAmount2 zero(String currency) {
        return new CustomerAmount2(BigDecimal.ZERO, currency);
    }
}
