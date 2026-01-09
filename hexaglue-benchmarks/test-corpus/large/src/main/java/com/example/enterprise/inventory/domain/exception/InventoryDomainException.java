package com.example.enterprise.inventory.domain.exception;

/**
 * Base exception for inventory domain errors.
 */
public class InventoryDomainException extends RuntimeException {
    public InventoryDomainException(String message) {
        super(message);
    }

    public InventoryDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
