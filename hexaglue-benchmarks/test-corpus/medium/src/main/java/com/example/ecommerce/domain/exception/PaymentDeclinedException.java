package com.example.ecommerce.domain.exception;

/**
 * Exception thrown when Payment declined.
 */
public class PaymentDeclinedException extends DomainException {
    public PaymentDeclinedException(String message) {
        super(message);
    }
}
