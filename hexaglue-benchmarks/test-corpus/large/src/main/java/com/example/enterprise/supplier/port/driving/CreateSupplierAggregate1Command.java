package com.example.enterprise.supplier.port.driving;

import java.util.List;

/**
 * Command to create a new SupplierAggregate1.
 */
public record CreateSupplierAggregate1Command(
    String name,
    List<String> itemDescriptions
) {
    public CreateSupplierAggregate1Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
