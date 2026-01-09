package com.example.enterprise.warehouse.port.driving;

import com.example.enterprise.warehouse.domain.model.WarehouseAggregate1Id;

/**
 * Command to update an existing WarehouseAggregate1.
 */
public record UpdateWarehouseAggregate1Command(
    WarehouseAggregate1Id id,
    String name
) {
    public UpdateWarehouseAggregate1Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
