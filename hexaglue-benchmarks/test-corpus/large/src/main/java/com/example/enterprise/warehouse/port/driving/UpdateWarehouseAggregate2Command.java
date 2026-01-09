package com.example.enterprise.warehouse.port.driving;

import com.example.enterprise.warehouse.domain.model.WarehouseAggregate2Id;

/**
 * Command to update an existing WarehouseAggregate2.
 */
public record UpdateWarehouseAggregate2Command(
    WarehouseAggregate2Id id,
    String name
) {
    public UpdateWarehouseAggregate2Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
