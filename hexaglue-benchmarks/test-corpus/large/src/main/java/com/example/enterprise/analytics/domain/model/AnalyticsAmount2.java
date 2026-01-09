package com.example.enterprise.analytics.domain.model;

import java.math.BigDecimal;

/**
 * Value Object representing an amount in analytics context.
 */
public record AnalyticsAmount2(BigDecimal value, String currency) {
    public AnalyticsAmount2 {
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

    public AnalyticsAmount2 add(AnalyticsAmount2 other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add amounts with different currencies");
        }
        return new AnalyticsAmount2(this.value.add(other.value), this.currency);
    }

    public static AnalyticsAmount2 zero(String currency) {
        return new AnalyticsAmount2(BigDecimal.ZERO, currency);
    }
}
