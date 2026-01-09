package com.example.enterprise.customer.domain.event;

import com.example.enterprise.customer.domain.model.CustomerAggregate1Id;

/**
 * Event fired when a CustomerAggregate1 is created.
 */
public class CustomerCreatedEvent extends CustomerEvent {
    private final CustomerAggregate1Id aggregateId;
    private final String name;

    public CustomerCreatedEvent(CustomerAggregate1Id aggregateId, String name) {
        super();
        this.aggregateId = aggregateId;
        this.name = name;
    }

    public CustomerAggregate1Id getAggregateId() { return aggregateId; }
    public String getName() { return name; }

    @Override
    public String getEventType() {
        return "CustomerCreated";
    }
}
