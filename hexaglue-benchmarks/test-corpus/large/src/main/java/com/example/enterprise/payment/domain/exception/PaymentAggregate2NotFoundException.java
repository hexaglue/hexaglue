package com.example.enterprise.payment.domain.exception;

import com.example.enterprise.payment.domain.model.PaymentAggregate2Id;

/**
 * Exception thrown when a PaymentAggregate2 is not found.
 */
public class PaymentAggregate2NotFoundException extends PaymentDomainException {
    public PaymentAggregate2NotFoundException(PaymentAggregate2Id id) {
        super("PaymentAggregate2 not found with id: " + id.value());
    }
}
