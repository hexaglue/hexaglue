package com.example.enterprise.supplier.port.driving;

import com.example.enterprise.supplier.domain.model.SupplierAggregate2Id;

/**
 * Command to update an existing SupplierAggregate2.
 */
public record UpdateSupplierAggregate2Command(
    SupplierAggregate2Id id,
    String name
) {
    public UpdateSupplierAggregate2Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
