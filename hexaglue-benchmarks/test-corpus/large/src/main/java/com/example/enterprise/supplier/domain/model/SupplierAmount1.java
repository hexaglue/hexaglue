package com.example.enterprise.supplier.domain.model;

import java.math.BigDecimal;

/**
 * Value Object representing an amount in supplier context.
 */
public record SupplierAmount1(BigDecimal value, String currency) {
    public SupplierAmount1 {
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

    public SupplierAmount1 add(SupplierAmount1 other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add amounts with different currencies");
        }
        return new SupplierAmount1(this.value.add(other.value), this.currency);
    }

    public static SupplierAmount1 zero(String currency) {
        return new SupplierAmount1(BigDecimal.ZERO, currency);
    }
}
