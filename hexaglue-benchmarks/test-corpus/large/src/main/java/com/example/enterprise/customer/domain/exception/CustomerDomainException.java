package com.example.enterprise.customer.domain.exception;

/**
 * Base exception for customer domain errors.
 */
public class CustomerDomainException extends RuntimeException {
    public CustomerDomainException(String message) {
        super(message);
    }

    public CustomerDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
