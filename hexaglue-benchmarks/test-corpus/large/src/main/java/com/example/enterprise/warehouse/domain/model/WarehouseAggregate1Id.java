package com.example.enterprise.warehouse.domain.model;

import java.util.UUID;

/**
 * Value Object representing a WarehouseAggregate1 identifier.
 */
public record WarehouseAggregate1Id(UUID value) {
    public WarehouseAggregate1Id {
        if (value == null) {
            throw new IllegalArgumentException("WarehouseAggregate1Id cannot be null");
        }
    }

    public static WarehouseAggregate1Id generate() {
        return new WarehouseAggregate1Id(UUID.randomUUID());
    }

    public static WarehouseAggregate1Id of(String value) {
        return new WarehouseAggregate1Id(UUID.fromString(value));
    }
}
