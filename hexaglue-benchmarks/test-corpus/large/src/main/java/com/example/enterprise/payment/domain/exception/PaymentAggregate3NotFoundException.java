package com.example.enterprise.payment.domain.exception;

import com.example.enterprise.payment.domain.model.PaymentAggregate3Id;

/**
 * Exception thrown when a PaymentAggregate3 is not found.
 */
public class PaymentAggregate3NotFoundException extends PaymentDomainException {
    public PaymentAggregate3NotFoundException(PaymentAggregate3Id id) {
        super("PaymentAggregate3 not found with id: " + id.value());
    }
}
