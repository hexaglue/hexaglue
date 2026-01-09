package com.example.enterprise.supplier.domain.exception;

import com.example.enterprise.supplier.domain.model.SupplierAggregate1Id;

/**
 * Exception thrown when a SupplierAggregate1 is not found.
 */
public class SupplierAggregate1NotFoundException extends SupplierDomainException {
    public SupplierAggregate1NotFoundException(SupplierAggregate1Id id) {
        super("SupplierAggregate1 not found with id: " + id.value());
    }
}
