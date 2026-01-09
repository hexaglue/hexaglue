package com.example.enterprise.inventory.domain.event;

import com.example.enterprise.inventory.domain.model.InventoryAggregate1Id;

/**
 * Event fired when a InventoryAggregate1 is created.
 */
public class InventoryCreatedEvent extends InventoryEvent {
    private final InventoryAggregate1Id aggregateId;
    private final String name;

    public InventoryCreatedEvent(InventoryAggregate1Id aggregateId, String name) {
        super();
        this.aggregateId = aggregateId;
        this.name = name;
    }

    public InventoryAggregate1Id getAggregateId() { return aggregateId; }
    public String getName() { return name; }

    @Override
    public String getEventType() {
        return "InventoryCreated";
    }
}
