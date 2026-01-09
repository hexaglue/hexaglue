package com.example.enterprise.analytics.domain.model;

import java.util.UUID;

public record AnalyticsItem2Id(UUID value) {
    public AnalyticsItem2Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static AnalyticsItem2Id generate() {
        return new AnalyticsItem2Id(UUID.randomUUID());
    }
}
