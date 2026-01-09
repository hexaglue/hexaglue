package com.example.enterprise.supplier.domain.exception;

import com.example.enterprise.supplier.domain.model.SupplierAggregate2Id;

/**
 * Exception thrown when a SupplierAggregate2 is not found.
 */
public class SupplierAggregate2NotFoundException extends SupplierDomainException {
    public SupplierAggregate2NotFoundException(SupplierAggregate2Id id) {
        super("SupplierAggregate2 not found with id: " + id.value());
    }
}
