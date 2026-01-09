package com.example.enterprise.analytics.domain.model;

import java.time.Instant;

/**
 * Value Object representing a timestamp in analytics context.
 */
public record AnalyticsTimestamp(Instant value) {
    public AnalyticsTimestamp {
        if (value == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
    }

    public static AnalyticsTimestamp now() {
        return new AnalyticsTimestamp(Instant.now());
    }

    public boolean isBefore(AnalyticsTimestamp other) {
        return this.value.isBefore(other.value);
    }

    public boolean isAfter(AnalyticsTimestamp other) {
        return this.value.isAfter(other.value);
    }
}
