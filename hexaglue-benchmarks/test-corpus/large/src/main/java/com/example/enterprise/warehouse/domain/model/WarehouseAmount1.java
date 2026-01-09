package com.example.enterprise.warehouse.domain.model;

import java.math.BigDecimal;

/**
 * Value Object representing an amount in warehouse context.
 */
public record WarehouseAmount1(BigDecimal value, String currency) {
    public WarehouseAmount1 {
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

    public WarehouseAmount1 add(WarehouseAmount1 other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add amounts with different currencies");
        }
        return new WarehouseAmount1(this.value.add(other.value), this.currency);
    }

    public static WarehouseAmount1 zero(String currency) {
        return new WarehouseAmount1(BigDecimal.ZERO, currency);
    }
}
