package com.example.enterprise.analytics.domain.model;

import java.math.BigDecimal;

/**
 * Value Object representing an amount in analytics context.
 */
public record AnalyticsAmount3(BigDecimal value, String currency) {
    public AnalyticsAmount3 {
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

    public AnalyticsAmount3 add(AnalyticsAmount3 other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add amounts with different currencies");
        }
        return new AnalyticsAmount3(this.value.add(other.value), this.currency);
    }

    public static AnalyticsAmount3 zero(String currency) {
        return new AnalyticsAmount3(BigDecimal.ZERO, currency);
    }
}
