package com.example.enterprise.analytics.port.driving;

import java.util.List;

/**
 * Command to create a new AnalyticsAggregate1.
 */
public record CreateAnalyticsAggregate1Command(
    String name,
    List<String> itemDescriptions
) {
    public CreateAnalyticsAggregate1Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
