package com.example.enterprise.marketing.domain.model;

import java.math.BigDecimal;

/**
 * Value Object representing an amount in marketing context.
 */
public record MarketingAmount1(BigDecimal value, String currency) {
    public MarketingAmount1 {
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

    public MarketingAmount1 add(MarketingAmount1 other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add amounts with different currencies");
        }
        return new MarketingAmount1(this.value.add(other.value), this.currency);
    }

    public static MarketingAmount1 zero(String currency) {
        return new MarketingAmount1(BigDecimal.ZERO, currency);
    }
}
