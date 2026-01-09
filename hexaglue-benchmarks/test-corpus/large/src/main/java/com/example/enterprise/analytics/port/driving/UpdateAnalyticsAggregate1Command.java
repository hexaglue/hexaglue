package com.example.enterprise.analytics.port.driving;

import com.example.enterprise.analytics.domain.model.AnalyticsAggregate1Id;

/**
 * Command to update an existing AnalyticsAggregate1.
 */
public record UpdateAnalyticsAggregate1Command(
    AnalyticsAggregate1Id id,
    String name
) {
    public UpdateAnalyticsAggregate1Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
