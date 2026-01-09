package com.example.enterprise.payment.port.driving;

import com.example.enterprise.payment.domain.model.PaymentAggregate2Id;

/**
 * Command to update an existing PaymentAggregate2.
 */
public record UpdatePaymentAggregate2Command(
    PaymentAggregate2Id id,
    String name
) {
    public UpdatePaymentAggregate2Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
