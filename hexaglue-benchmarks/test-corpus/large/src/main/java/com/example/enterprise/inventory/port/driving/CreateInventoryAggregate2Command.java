package com.example.enterprise.inventory.port.driving;

import java.util.List;

/**
 * Command to create a new InventoryAggregate2.
 */
public record CreateInventoryAggregate2Command(
    String name,
    List<String> itemDescriptions
) {
    public CreateInventoryAggregate2Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
