package com.example.enterprise.ordering.domain.event;

import com.example.enterprise.ordering.domain.model.OrderingAggregate1Id;

/**
 * Event fired when a OrderingAggregate1 is updated.
 */
public class OrderingUpdatedEvent extends OrderingEvent {
    private final OrderingAggregate1Id aggregateId;

    public OrderingUpdatedEvent(OrderingAggregate1Id aggregateId) {
        super();
        this.aggregateId = aggregateId;
    }

    public OrderingAggregate1Id getAggregateId() { return aggregateId; }

    @Override
    public String getEventType() {
        return "OrderingUpdated";
    }
}
