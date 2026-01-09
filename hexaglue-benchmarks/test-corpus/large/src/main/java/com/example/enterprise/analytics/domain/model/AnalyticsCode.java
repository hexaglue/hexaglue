package com.example.enterprise.analytics.domain.model;

/**
 * Value Object representing a code in analytics context.
 */
public record AnalyticsCode(String value) {
    public AnalyticsCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Code cannot be null or blank");
        }
        if (value.length() > 50) {
            throw new IllegalArgumentException("Code cannot exceed 50 characters");
        }
    }
}
