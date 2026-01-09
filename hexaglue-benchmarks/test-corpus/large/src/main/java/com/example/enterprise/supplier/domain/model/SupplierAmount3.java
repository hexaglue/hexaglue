package com.example.enterprise.supplier.domain.model;

import java.math.BigDecimal;

/**
 * Value Object representing an amount in supplier context.
 */
public record SupplierAmount3(BigDecimal value, String currency) {
    public SupplierAmount3 {
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

    public SupplierAmount3 add(SupplierAmount3 other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add amounts with different currencies");
        }
        return new SupplierAmount3(this.value.add(other.value), this.currency);
    }

    public static SupplierAmount3 zero(String currency) {
        return new SupplierAmount3(BigDecimal.ZERO, currency);
    }
}
