package com.example.enterprise.payment.domain.event;

import com.example.enterprise.payment.domain.model.PaymentAggregate1Id;

/**
 * Event fired when a PaymentAggregate1 is updated.
 */
public class PaymentUpdatedEvent extends PaymentEvent {
    private final PaymentAggregate1Id aggregateId;

    public PaymentUpdatedEvent(PaymentAggregate1Id aggregateId) {
        super();
        this.aggregateId = aggregateId;
    }

    public PaymentAggregate1Id getAggregateId() { return aggregateId; }

    @Override
    public String getEventType() {
        return "PaymentUpdated";
    }
}
