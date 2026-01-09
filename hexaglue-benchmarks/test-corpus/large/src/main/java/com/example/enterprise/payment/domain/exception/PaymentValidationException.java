package com.example.enterprise.payment.domain.exception;

/**
 * Exception thrown when validation fails in payment context.
 */
public class PaymentValidationException extends PaymentDomainException {
    public PaymentValidationException(String message) {
        super(message);
    }
}
