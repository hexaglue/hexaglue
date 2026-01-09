package com.example.enterprise.marketing.domain.event;

import com.example.enterprise.marketing.domain.model.MarketingAggregate1Id;

/**
 * Event fired when a MarketingAggregate1 is created.
 */
public class MarketingCreatedEvent extends MarketingEvent {
    private final MarketingAggregate1Id aggregateId;
    private final String name;

    public MarketingCreatedEvent(MarketingAggregate1Id aggregateId, String name) {
        super();
        this.aggregateId = aggregateId;
        this.name = name;
    }

    public MarketingAggregate1Id getAggregateId() { return aggregateId; }
    public String getName() { return name; }

    @Override
    public String getEventType() {
        return "MarketingCreated";
    }
}
