package com.example.enterprise.payment.port.driving;

import com.example.enterprise.payment.domain.model.PaymentAggregate1Id;

/**
 * Command to update an existing PaymentAggregate1.
 */
public record UpdatePaymentAggregate1Command(
    PaymentAggregate1Id id,
    String name
) {
    public UpdatePaymentAggregate1Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
