package com.example.enterprise.inventory.port.driving;

import java.util.List;

/**
 * Command to create a new InventoryAggregate3.
 */
public record CreateInventoryAggregate3Command(
    String name,
    List<String> itemDescriptions
) {
    public CreateInventoryAggregate3Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
