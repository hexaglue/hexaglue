package com.example.enterprise.inventory.domain.model;

import java.math.BigDecimal;

/**
 * Value Object representing an amount in inventory context.
 */
public record InventoryAmount2(BigDecimal value, String currency) {
    public InventoryAmount2 {
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

    public InventoryAmount2 add(InventoryAmount2 other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add amounts with different currencies");
        }
        return new InventoryAmount2(this.value.add(other.value), this.currency);
    }

    public static InventoryAmount2 zero(String currency) {
        return new InventoryAmount2(BigDecimal.ZERO, currency);
    }
}
