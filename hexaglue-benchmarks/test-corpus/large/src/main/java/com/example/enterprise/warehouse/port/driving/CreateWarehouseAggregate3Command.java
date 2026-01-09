package com.example.enterprise.warehouse.port.driving;

import java.util.List;

/**
 * Command to create a new WarehouseAggregate3.
 */
public record CreateWarehouseAggregate3Command(
    String name,
    List<String> itemDescriptions
) {
    public CreateWarehouseAggregate3Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
