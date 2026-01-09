package com.example.enterprise.warehouse.domain.event;

import com.example.enterprise.warehouse.domain.model.WarehouseAggregate1Id;

/**
 * Event fired when a WarehouseAggregate1 is updated.
 */
public class WarehouseUpdatedEvent extends WarehouseEvent {
    private final WarehouseAggregate1Id aggregateId;

    public WarehouseUpdatedEvent(WarehouseAggregate1Id aggregateId) {
        super();
        this.aggregateId = aggregateId;
    }

    public WarehouseAggregate1Id getAggregateId() { return aggregateId; }

    @Override
    public String getEventType() {
        return "WarehouseUpdated";
    }
}
