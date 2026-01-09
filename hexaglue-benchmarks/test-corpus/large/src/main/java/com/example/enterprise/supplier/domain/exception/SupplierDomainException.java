package com.example.enterprise.supplier.domain.exception;

/**
 * Base exception for supplier domain errors.
 */
public class SupplierDomainException extends RuntimeException {
    public SupplierDomainException(String message) {
        super(message);
    }

    public SupplierDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
