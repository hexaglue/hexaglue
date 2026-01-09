package com.example.enterprise.analytics.domain.model;

import java.util.UUID;

public record AnalyticsItem3Id(UUID value) {
    public AnalyticsItem3Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static AnalyticsItem3Id generate() {
        return new AnalyticsItem3Id(UUID.randomUUID());
    }
}
