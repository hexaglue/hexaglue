package com.example.ecommerce.domain.exception;

import com.example.ecommerce.domain.model.SupplierId;

/**
 * Exception thrown when Supplier is not found.
 */
public class SupplierNotFoundException extends DomainException {
    private final SupplierId id;

    public SupplierNotFoundException(SupplierId id) {
        super("Supplier not found: " + id);
        this.id = id;
    }

    public SupplierId getId() {
        return id;
    }
}
