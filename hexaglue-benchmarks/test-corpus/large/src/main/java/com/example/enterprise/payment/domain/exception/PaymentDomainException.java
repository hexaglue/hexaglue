package com.example.enterprise.payment.domain.exception;

/**
 * Base exception for payment domain errors.
 */
public class PaymentDomainException extends RuntimeException {
    public PaymentDomainException(String message) {
        super(message);
    }

    public PaymentDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
