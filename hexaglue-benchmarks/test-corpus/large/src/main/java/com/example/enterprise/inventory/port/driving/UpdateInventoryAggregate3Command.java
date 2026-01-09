package com.example.enterprise.inventory.port.driving;

import com.example.enterprise.inventory.domain.model.InventoryAggregate3Id;

/**
 * Command to update an existing InventoryAggregate3.
 */
public record UpdateInventoryAggregate3Command(
    InventoryAggregate3Id id,
    String name
) {
    public UpdateInventoryAggregate3Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
