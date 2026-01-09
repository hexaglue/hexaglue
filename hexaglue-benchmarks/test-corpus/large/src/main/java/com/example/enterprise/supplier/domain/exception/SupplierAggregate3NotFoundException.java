package com.example.enterprise.supplier.domain.exception;

import com.example.enterprise.supplier.domain.model.SupplierAggregate3Id;

/**
 * Exception thrown when a SupplierAggregate3 is not found.
 */
public class SupplierAggregate3NotFoundException extends SupplierDomainException {
    public SupplierAggregate3NotFoundException(SupplierAggregate3Id id) {
        super("SupplierAggregate3 not found with id: " + id.value());
    }
}
