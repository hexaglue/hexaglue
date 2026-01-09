package com.example.enterprise.catalog.domain.model;

import java.math.BigDecimal;

/**
 * Value Object representing an amount in catalog context.
 */
public record CatalogAmount1(BigDecimal value, String currency) {
    public CatalogAmount1 {
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

    public CatalogAmount1 add(CatalogAmount1 other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add amounts with different currencies");
        }
        return new CatalogAmount1(this.value.add(other.value), this.currency);
    }

    public static CatalogAmount1 zero(String currency) {
        return new CatalogAmount1(BigDecimal.ZERO, currency);
    }
}
