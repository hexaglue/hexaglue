package com.example.enterprise.analytics.port.driving;

import com.example.enterprise.analytics.domain.model.AnalyticsAggregate3Id;

/**
 * Command to update an existing AnalyticsAggregate3.
 */
public record UpdateAnalyticsAggregate3Command(
    AnalyticsAggregate3Id id,
    String name
) {
    public UpdateAnalyticsAggregate3Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
