package com.example.enterprise.analytics.domain.model;

/**
 * Value Object representing a description in analytics context.
 */
public record AnalyticsDescription(String value) {
    public AnalyticsDescription {
        if (value != null && value.length() > 1000) {
            throw new IllegalArgumentException("Description cannot exceed 1000 characters");
        }
    }

    public static AnalyticsDescription empty() {
        return new AnalyticsDescription("");
    }

    public boolean isEmpty() {
        return value == null || value.isBlank();
    }
}
