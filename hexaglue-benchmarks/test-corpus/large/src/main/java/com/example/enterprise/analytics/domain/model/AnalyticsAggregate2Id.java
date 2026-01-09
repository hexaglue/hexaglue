package com.example.enterprise.analytics.domain.model;

import java.util.UUID;

/**
 * Value Object representing a AnalyticsAggregate2 identifier.
 */
public record AnalyticsAggregate2Id(UUID value) {
    public AnalyticsAggregate2Id {
        if (value == null) {
            throw new IllegalArgumentException("AnalyticsAggregate2Id cannot be null");
        }
    }

    public static AnalyticsAggregate2Id generate() {
        return new AnalyticsAggregate2Id(UUID.randomUUID());
    }

    public static AnalyticsAggregate2Id of(String value) {
        return new AnalyticsAggregate2Id(UUID.fromString(value));
    }
}
