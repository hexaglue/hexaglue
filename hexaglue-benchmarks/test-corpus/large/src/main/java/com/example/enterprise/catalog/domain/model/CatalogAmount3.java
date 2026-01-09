package com.example.enterprise.catalog.domain.model;

import java.math.BigDecimal;

/**
 * Value Object representing an amount in catalog context.
 */
public record CatalogAmount3(BigDecimal value, String currency) {
    public CatalogAmount3 {
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

    public CatalogAmount3 add(CatalogAmount3 other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add amounts with different currencies");
        }
        return new CatalogAmount3(this.value.add(other.value), this.currency);
    }

    public static CatalogAmount3 zero(String currency) {
        return new CatalogAmount3(BigDecimal.ZERO, currency);
    }
}
