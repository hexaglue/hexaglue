package com.example.enterprise.shipping.domain.event;

import com.example.enterprise.shipping.domain.model.ShippingAggregate1Id;

/**
 * Event fired when a ShippingAggregate1 is updated.
 */
public class ShippingUpdatedEvent extends ShippingEvent {
    private final ShippingAggregate1Id aggregateId;

    public ShippingUpdatedEvent(ShippingAggregate1Id aggregateId) {
        super();
        this.aggregateId = aggregateId;
    }

    public ShippingAggregate1Id getAggregateId() { return aggregateId; }

    @Override
    public String getEventType() {
        return "ShippingUpdated";
    }
}
