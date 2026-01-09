package com.example.enterprise.warehouse.port.driving;

import com.example.enterprise.warehouse.domain.model.WarehouseAggregate3Id;

/**
 * Command to update an existing WarehouseAggregate3.
 */
public record UpdateWarehouseAggregate3Command(
    WarehouseAggregate3Id id,
    String name
) {
    public UpdateWarehouseAggregate3Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
