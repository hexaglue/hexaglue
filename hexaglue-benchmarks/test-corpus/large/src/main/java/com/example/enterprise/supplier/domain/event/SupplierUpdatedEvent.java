package com.example.enterprise.supplier.domain.event;

import com.example.enterprise.supplier.domain.model.SupplierAggregate1Id;

/**
 * Event fired when a SupplierAggregate1 is updated.
 */
public class SupplierUpdatedEvent extends SupplierEvent {
    private final SupplierAggregate1Id aggregateId;

    public SupplierUpdatedEvent(SupplierAggregate1Id aggregateId) {
        super();
        this.aggregateId = aggregateId;
    }

    public SupplierAggregate1Id getAggregateId() { return aggregateId; }

    @Override
    public String getEventType() {
        return "SupplierUpdated";
    }
}
