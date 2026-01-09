package com.example.enterprise.ordering.domain.event;

import com.example.enterprise.ordering.domain.model.OrderingAggregate1Id;

/**
 * Event fired when a OrderingAggregate1 is created.
 */
public class OrderingCreatedEvent extends OrderingEvent {
    private final OrderingAggregate1Id aggregateId;
    private final String name;

    public OrderingCreatedEvent(OrderingAggregate1Id aggregateId, String name) {
        super();
        this.aggregateId = aggregateId;
        this.name = name;
    }

    public OrderingAggregate1Id getAggregateId() { return aggregateId; }
    public String getName() { return name; }

    @Override
    public String getEventType() {
        return "OrderingCreated";
    }
}
