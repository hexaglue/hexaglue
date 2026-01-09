package com.example.enterprise.warehouse.domain.exception;

/**
 * Base exception for warehouse domain errors.
 */
public class WarehouseDomainException extends RuntimeException {
    public WarehouseDomainException(String message) {
        super(message);
    }

    public WarehouseDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
