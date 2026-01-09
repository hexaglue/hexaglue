package com.example.enterprise.supplier.port.driving;

import com.example.enterprise.supplier.domain.model.SupplierAggregate1Id;

/**
 * Command to update an existing SupplierAggregate1.
 */
public record UpdateSupplierAggregate1Command(
    SupplierAggregate1Id id,
    String name
) {
    public UpdateSupplierAggregate1Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
