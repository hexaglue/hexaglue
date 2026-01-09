package com.example.enterprise.customer.domain.event;

import com.example.enterprise.customer.domain.model.CustomerAggregate1Id;

/**
 * Event fired when a CustomerAggregate1 is updated.
 */
public class CustomerUpdatedEvent extends CustomerEvent {
    private final CustomerAggregate1Id aggregateId;

    public CustomerUpdatedEvent(CustomerAggregate1Id aggregateId) {
        super();
        this.aggregateId = aggregateId;
    }

    public CustomerAggregate1Id getAggregateId() { return aggregateId; }

    @Override
    public String getEventType() {
        return "CustomerUpdated";
    }
}
