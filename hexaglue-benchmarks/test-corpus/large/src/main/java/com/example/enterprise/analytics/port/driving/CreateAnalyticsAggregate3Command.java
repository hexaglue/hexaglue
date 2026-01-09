package com.example.enterprise.analytics.port.driving;

import java.util.List;

/**
 * Command to create a new AnalyticsAggregate3.
 */
public record CreateAnalyticsAggregate3Command(
    String name,
    List<String> itemDescriptions
) {
    public CreateAnalyticsAggregate3Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
