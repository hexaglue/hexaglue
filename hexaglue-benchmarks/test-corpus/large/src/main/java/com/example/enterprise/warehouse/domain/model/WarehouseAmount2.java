package com.example.enterprise.warehouse.domain.model;

import java.math.BigDecimal;

/**
 * Value Object representing an amount in warehouse context.
 */
public record WarehouseAmount2(BigDecimal value, String currency) {
    public WarehouseAmount2 {
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

    public WarehouseAmount2 add(WarehouseAmount2 other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add amounts with different currencies");
        }
        return new WarehouseAmount2(this.value.add(other.value), this.currency);
    }

    public static WarehouseAmount2 zero(String currency) {
        return new WarehouseAmount2(BigDecimal.ZERO, currency);
    }
}
