package com.example.enterprise.marketing.domain.model;

import java.math.BigDecimal;

/**
 * Value Object representing an amount in marketing context.
 */
public record MarketingAmount3(BigDecimal value, String currency) {
    public MarketingAmount3 {
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

    public MarketingAmount3 add(MarketingAmount3 other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add amounts with different currencies");
        }
        return new MarketingAmount3(this.value.add(other.value), this.currency);
    }

    public static MarketingAmount3 zero(String currency) {
        return new MarketingAmount3(BigDecimal.ZERO, currency);
    }
}
