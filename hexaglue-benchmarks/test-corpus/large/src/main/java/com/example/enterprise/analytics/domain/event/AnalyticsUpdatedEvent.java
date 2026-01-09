package com.example.enterprise.analytics.domain.event;

import com.example.enterprise.analytics.domain.model.AnalyticsAggregate1Id;

/**
 * Event fired when a AnalyticsAggregate1 is updated.
 */
public class AnalyticsUpdatedEvent extends AnalyticsEvent {
    private final AnalyticsAggregate1Id aggregateId;

    public AnalyticsUpdatedEvent(AnalyticsAggregate1Id aggregateId) {
        super();
        this.aggregateId = aggregateId;
    }

    public AnalyticsAggregate1Id getAggregateId() { return aggregateId; }

    @Override
    public String getEventType() {
        return "AnalyticsUpdated";
    }
}
