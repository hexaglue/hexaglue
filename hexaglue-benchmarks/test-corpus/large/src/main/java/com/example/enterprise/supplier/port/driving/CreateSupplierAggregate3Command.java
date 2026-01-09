package com.example.enterprise.supplier.port.driving;

import java.util.List;

/**
 * Command to create a new SupplierAggregate3.
 */
public record CreateSupplierAggregate3Command(
    String name,
    List<String> itemDescriptions
) {
    public CreateSupplierAggregate3Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
