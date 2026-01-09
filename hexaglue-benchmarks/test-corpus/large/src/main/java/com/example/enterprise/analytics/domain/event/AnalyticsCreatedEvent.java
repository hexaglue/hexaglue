package com.example.enterprise.analytics.domain.event;

import com.example.enterprise.analytics.domain.model.AnalyticsAggregate1Id;

/**
 * Event fired when a AnalyticsAggregate1 is created.
 */
public class AnalyticsCreatedEvent extends AnalyticsEvent {
    private final AnalyticsAggregate1Id aggregateId;
    private final String name;

    public AnalyticsCreatedEvent(AnalyticsAggregate1Id aggregateId, String name) {
        super();
        this.aggregateId = aggregateId;
        this.name = name;
    }

    public AnalyticsAggregate1Id getAggregateId() { return aggregateId; }
    public String getName() { return name; }

    @Override
    public String getEventType() {
        return "AnalyticsCreated";
    }
}
