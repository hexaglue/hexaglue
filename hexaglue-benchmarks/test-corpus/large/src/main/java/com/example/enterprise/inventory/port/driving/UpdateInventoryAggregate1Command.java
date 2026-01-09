package com.example.enterprise.inventory.port.driving;

import com.example.enterprise.inventory.domain.model.InventoryAggregate1Id;

/**
 * Command to update an existing InventoryAggregate1.
 */
public record UpdateInventoryAggregate1Command(
    InventoryAggregate1Id id,
    String name
) {
    public UpdateInventoryAggregate1Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
