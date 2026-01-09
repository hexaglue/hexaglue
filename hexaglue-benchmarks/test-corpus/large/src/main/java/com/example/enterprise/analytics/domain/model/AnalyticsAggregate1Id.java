package com.example.enterprise.analytics.domain.model;

import java.util.UUID;

/**
 * Value Object representing a AnalyticsAggregate1 identifier.
 */
public record AnalyticsAggregate1Id(UUID value) {
    public AnalyticsAggregate1Id {
        if (value == null) {
            throw new IllegalArgumentException("AnalyticsAggregate1Id cannot be null");
        }
    }

    public static AnalyticsAggregate1Id generate() {
        return new AnalyticsAggregate1Id(UUID.randomUUID());
    }

    public static AnalyticsAggregate1Id of(String value) {
        return new AnalyticsAggregate1Id(UUID.fromString(value));
    }
}
