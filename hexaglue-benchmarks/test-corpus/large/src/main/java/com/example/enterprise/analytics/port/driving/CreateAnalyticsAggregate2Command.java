package com.example.enterprise.analytics.port.driving;

import java.util.List;

/**
 * Command to create a new AnalyticsAggregate2.
 */
public record CreateAnalyticsAggregate2Command(
    String name,
    List<String> itemDescriptions
) {
    public CreateAnalyticsAggregate2Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
