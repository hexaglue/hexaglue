package com.example.enterprise.supplier.domain.event;

import com.example.enterprise.supplier.domain.model.SupplierAggregate1Id;

/**
 * Event fired when a SupplierAggregate1 is created.
 */
public class SupplierCreatedEvent extends SupplierEvent {
    private final SupplierAggregate1Id aggregateId;
    private final String name;

    public SupplierCreatedEvent(SupplierAggregate1Id aggregateId, String name) {
        super();
        this.aggregateId = aggregateId;
        this.name = name;
    }

    public SupplierAggregate1Id getAggregateId() { return aggregateId; }
    public String getName() { return name; }

    @Override
    public String getEventType() {
        return "SupplierCreated";
    }
}
