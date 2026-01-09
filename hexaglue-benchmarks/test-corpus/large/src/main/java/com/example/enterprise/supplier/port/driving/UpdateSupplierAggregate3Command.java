package com.example.enterprise.supplier.port.driving;

import com.example.enterprise.supplier.domain.model.SupplierAggregate3Id;

/**
 * Command to update an existing SupplierAggregate3.
 */
public record UpdateSupplierAggregate3Command(
    SupplierAggregate3Id id,
    String name
) {
    public UpdateSupplierAggregate3Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
