package com.example.enterprise.analytics.port.driving;

import com.example.enterprise.analytics.domain.model.AnalyticsAggregate2Id;

/**
 * Command to update an existing AnalyticsAggregate2.
 */
public record UpdateAnalyticsAggregate2Command(
    AnalyticsAggregate2Id id,
    String name
) {
    public UpdateAnalyticsAggregate2Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
