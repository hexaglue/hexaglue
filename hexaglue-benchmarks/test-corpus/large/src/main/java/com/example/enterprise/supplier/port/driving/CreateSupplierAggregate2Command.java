package com.example.enterprise.supplier.port.driving;

import java.util.List;

/**
 * Command to create a new SupplierAggregate2.
 */
public record CreateSupplierAggregate2Command(
    String name,
    List<String> itemDescriptions
) {
    public CreateSupplierAggregate2Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
