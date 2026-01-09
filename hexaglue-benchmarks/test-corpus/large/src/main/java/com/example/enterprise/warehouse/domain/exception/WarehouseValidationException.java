package com.example.enterprise.warehouse.domain.exception;

/**
 * Exception thrown when validation fails in warehouse context.
 */
public class WarehouseValidationException extends WarehouseDomainException {
    public WarehouseValidationException(String message) {
        super(message);
    }
}
