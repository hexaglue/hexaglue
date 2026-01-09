package com.example.enterprise.supplier.domain.model;

import java.math.BigDecimal;

/**
 * Value Object representing an amount in supplier context.
 */
public record SupplierAmount2(BigDecimal value, String currency) {
    public SupplierAmount2 {
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

    public SupplierAmount2 add(SupplierAmount2 other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add amounts with different currencies");
        }
        return new SupplierAmount2(this.value.add(other.value), this.currency);
    }

    public static SupplierAmount2 zero(String currency) {
        return new SupplierAmount2(BigDecimal.ZERO, currency);
    }
}
