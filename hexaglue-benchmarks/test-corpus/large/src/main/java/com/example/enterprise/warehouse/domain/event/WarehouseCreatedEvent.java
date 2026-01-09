package com.example.enterprise.warehouse.domain.event;

import com.example.enterprise.warehouse.domain.model.WarehouseAggregate1Id;

/**
 * Event fired when a WarehouseAggregate1 is created.
 */
public class WarehouseCreatedEvent extends WarehouseEvent {
    private final WarehouseAggregate1Id aggregateId;
    private final String name;

    public WarehouseCreatedEvent(WarehouseAggregate1Id aggregateId, String name) {
        super();
        this.aggregateId = aggregateId;
        this.name = name;
    }

    public WarehouseAggregate1Id getAggregateId() { return aggregateId; }
    public String getName() { return name; }

    @Override
    public String getEventType() {
        return "WarehouseCreated";
    }
}
