package com.example.enterprise.shipping.domain.model;

import java.math.BigDecimal;

/**
 * Value Object representing an amount in shipping context.
 */
public record ShippingAmount1(BigDecimal value, String currency) {
    public ShippingAmount1 {
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

    public ShippingAmount1 add(ShippingAmount1 other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add amounts with different currencies");
        }
        return new ShippingAmount1(this.value.add(other.value), this.currency);
    }

    public static ShippingAmount1 zero(String currency) {
        return new ShippingAmount1(BigDecimal.ZERO, currency);
    }
}
