package com.example.enterprise.analytics.domain.model;

import java.util.UUID;

/**
 * Value Object representing a AnalyticsAggregate3 identifier.
 */
public record AnalyticsAggregate3Id(UUID value) {
    public AnalyticsAggregate3Id {
        if (value == null) {
            throw new IllegalArgumentException("AnalyticsAggregate3Id cannot be null");
        }
    }

    public static AnalyticsAggregate3Id generate() {
        return new AnalyticsAggregate3Id(UUID.randomUUID());
    }

    public static AnalyticsAggregate3Id of(String value) {
        return new AnalyticsAggregate3Id(UUID.fromString(value));
    }
}
