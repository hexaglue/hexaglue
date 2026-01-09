package com.example.enterprise.customer.domain.exception;

/**
 * Exception thrown when validation fails in customer context.
 */
public class CustomerValidationException extends CustomerDomainException {
    public CustomerValidationException(String message) {
        super(message);
    }
}
