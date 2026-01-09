package com.example.enterprise.payment.domain.event;

import com.example.enterprise.payment.domain.model.PaymentAggregate1Id;

/**
 * Event fired when a PaymentAggregate1 is created.
 */
public class PaymentCreatedEvent extends PaymentEvent {
    private final PaymentAggregate1Id aggregateId;
    private final String name;

    public PaymentCreatedEvent(PaymentAggregate1Id aggregateId, String name) {
        super();
        this.aggregateId = aggregateId;
        this.name = name;
    }

    public PaymentAggregate1Id getAggregateId() { return aggregateId; }
    public String getName() { return name; }

    @Override
    public String getEventType() {
        return "PaymentCreated";
    }
}
