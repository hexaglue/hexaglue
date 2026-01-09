package com.example.enterprise.shipping.domain.event;

import com.example.enterprise.shipping.domain.model.ShippingAggregate1Id;

/**
 * Event fired when a ShippingAggregate1 is created.
 */
public class ShippingCreatedEvent extends ShippingEvent {
    private final ShippingAggregate1Id aggregateId;
    private final String name;

    public ShippingCreatedEvent(ShippingAggregate1Id aggregateId, String name) {
        super();
        this.aggregateId = aggregateId;
        this.name = name;
    }

    public ShippingAggregate1Id getAggregateId() { return aggregateId; }
    public String getName() { return name; }

    @Override
    public String getEventType() {
        return "ShippingCreated";
    }
}
