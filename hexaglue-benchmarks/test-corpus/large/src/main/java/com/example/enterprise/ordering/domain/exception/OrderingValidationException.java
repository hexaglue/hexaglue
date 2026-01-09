package com.example.enterprise.ordering.domain.exception;

/**
 * Exception thrown when validation fails in ordering context.
 */
public class OrderingValidationException extends OrderingDomainException {
    public OrderingValidationException(String message) {
        super(message);
    }
}
