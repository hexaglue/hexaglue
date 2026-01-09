package com.example.enterprise.supplier.domain.exception;

/**
 * Exception thrown when validation fails in supplier context.
 */
public class SupplierValidationException extends SupplierDomainException {
    public SupplierValidationException(String message) {
        super(message);
    }
}
