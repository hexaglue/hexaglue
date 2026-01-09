package com.example.enterprise.warehouse.port.driving;

import java.util.List;

/**
 * Command to create a new WarehouseAggregate2.
 */
public record CreateWarehouseAggregate2Command(
    String name,
    List<String> itemDescriptions
) {
    public CreateWarehouseAggregate2Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
