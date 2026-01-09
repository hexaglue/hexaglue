package com.example.enterprise.inventory.port.driving;

import java.util.List;

/**
 * Command to create a new InventoryAggregate1.
 */
public record CreateInventoryAggregate1Command(
    String name,
    List<String> itemDescriptions
) {
    public CreateInventoryAggregate1Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
