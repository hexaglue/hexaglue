package com.example.enterprise.analytics.domain.model;

import java.util.UUID;

public record AnalyticsItem1Id(UUID value) {
    public AnalyticsItem1Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static AnalyticsItem1Id generate() {
        return new AnalyticsItem1Id(UUID.randomUUID());
    }
}
