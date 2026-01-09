package com.example.ecommerce.domain.exception;

/**
 * Exception thrown when Insufficient inventory.
 */
public class InsufficientInventoryException extends DomainException {
    public InsufficientInventoryException(String message) {
        super(message);
    }
}
