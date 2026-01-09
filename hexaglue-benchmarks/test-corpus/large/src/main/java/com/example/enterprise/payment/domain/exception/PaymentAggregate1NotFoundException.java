package com.example.enterprise.payment.domain.exception;

import com.example.enterprise.payment.domain.model.PaymentAggregate1Id;

/**
 * Exception thrown when a PaymentAggregate1 is not found.
 */
public class PaymentAggregate1NotFoundException extends PaymentDomainException {
    public PaymentAggregate1NotFoundException(PaymentAggregate1Id id) {
        super("PaymentAggregate1 not found with id: " + id.value());
    }
}
