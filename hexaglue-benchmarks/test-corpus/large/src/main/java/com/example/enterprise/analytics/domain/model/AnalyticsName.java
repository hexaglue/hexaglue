package com.example.enterprise.analytics.domain.model;

/**
 * Value Object representing a name in analytics context.
 */
public record AnalyticsName(String value) {
    public AnalyticsName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        if (value.length() > 200) {
            throw new IllegalArgumentException("Name cannot exceed 200 characters");
        }
    }
}
