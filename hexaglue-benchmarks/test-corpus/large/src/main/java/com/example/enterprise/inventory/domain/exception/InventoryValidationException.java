package com.example.enterprise.inventory.domain.exception;

/**
 * Exception thrown when validation fails in inventory context.
 */
public class InventoryValidationException extends InventoryDomainException {
    public InventoryValidationException(String message) {
        super(message);
    }
}
