package com.example.enterprise.inventory.port.driving;

import com.example.enterprise.inventory.domain.model.InventoryAggregate2Id;

/**
 * Command to update an existing InventoryAggregate2.
 */
public record UpdateInventoryAggregate2Command(
    InventoryAggregate2Id id,
    String name
) {
    public UpdateInventoryAggregate2Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
