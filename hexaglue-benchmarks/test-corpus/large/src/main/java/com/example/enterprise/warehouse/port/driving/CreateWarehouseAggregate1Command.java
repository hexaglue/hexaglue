package com.example.enterprise.warehouse.port.driving;

import java.util.List;

/**
 * Command to create a new WarehouseAggregate1.
 */
public record CreateWarehouseAggregate1Command(
    String name,
    List<String> itemDescriptions
) {
    public CreateWarehouseAggregate1Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
