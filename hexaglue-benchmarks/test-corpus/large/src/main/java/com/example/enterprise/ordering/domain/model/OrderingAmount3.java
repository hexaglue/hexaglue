package com.example.enterprise.ordering.domain.model;

import java.math.BigDecimal;

/**
 * Value Object representing an amount in ordering context.
 */
public record OrderingAmount3(BigDecimal value, String currency) {
    public OrderingAmount3 {
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

    public OrderingAmount3 add(OrderingAmount3 other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add amounts with different currencies");
        }
        return new OrderingAmount3(this.value.add(other.value), this.currency);
    }

    public static OrderingAmount3 zero(String currency) {
        return new OrderingAmount3(BigDecimal.ZERO, currency);
    }
}
