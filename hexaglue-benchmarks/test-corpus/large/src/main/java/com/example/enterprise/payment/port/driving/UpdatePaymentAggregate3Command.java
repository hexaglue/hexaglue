package com.example.enterprise.payment.port.driving;

import com.example.enterprise.payment.domain.model.PaymentAggregate3Id;

/**
 * Command to update an existing PaymentAggregate3.
 */
public record UpdatePaymentAggregate3Command(
    PaymentAggregate3Id id,
    String name
) {
    public UpdatePaymentAggregate3Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
