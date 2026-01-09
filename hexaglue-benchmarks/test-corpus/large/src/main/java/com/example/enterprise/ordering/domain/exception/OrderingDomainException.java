package com.example.enterprise.ordering.domain.exception;

/**
 * Base exception for ordering domain errors.
 */
public class OrderingDomainException extends RuntimeException {
    public OrderingDomainException(String message) {
        super(message);
    }

    public OrderingDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
