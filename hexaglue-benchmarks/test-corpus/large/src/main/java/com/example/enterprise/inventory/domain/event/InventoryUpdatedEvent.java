package com.example.enterprise.inventory.domain.event;

import com.example.enterprise.inventory.domain.model.InventoryAggregate1Id;

/**
 * Event fired when a InventoryAggregate1 is updated.
 */
public class InventoryUpdatedEvent extends InventoryEvent {
    private final InventoryAggregate1Id aggregateId;

    public InventoryUpdatedEvent(InventoryAggregate1Id aggregateId) {
        super();
        this.aggregateId = aggregateId;
    }

    public InventoryAggregate1Id getAggregateId() { return aggregateId; }

    @Override
    public String getEventType() {
        return "InventoryUpdated";
    }
}
