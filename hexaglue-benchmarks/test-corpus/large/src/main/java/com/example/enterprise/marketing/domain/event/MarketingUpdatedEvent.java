package com.example.enterprise.marketing.domain.event;

import com.example.enterprise.marketing.domain.model.MarketingAggregate1Id;

/**
 * Event fired when a MarketingAggregate1 is updated.
 */
public class MarketingUpdatedEvent extends MarketingEvent {
    private final MarketingAggregate1Id aggregateId;

    public MarketingUpdatedEvent(MarketingAggregate1Id aggregateId) {
        super();
        this.aggregateId = aggregateId;
    }

    public MarketingAggregate1Id getAggregateId() { return aggregateId; }

    @Override
    public String getEventType() {
        return "MarketingUpdated";
    }
}
