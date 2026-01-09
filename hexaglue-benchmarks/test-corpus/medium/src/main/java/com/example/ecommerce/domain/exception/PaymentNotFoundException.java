package com.example.ecommerce.domain.exception;

import com.example.ecommerce.domain.model.PaymentId;

/**
 * Exception thrown when Payment is not found.
 */
public class PaymentNotFoundException extends DomainException {
    private final PaymentId id;

    public PaymentNotFoundException(PaymentId id) {
        super("Payment not found: " + id);
        this.id = id;
    }

    public PaymentId getId() {
        return id;
    }
}
