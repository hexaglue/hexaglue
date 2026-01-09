package com.example.enterprise.warehouse.domain.model;

import java.util.UUID;

/**
 * Value Object representing a WarehouseAggregate2 identifier.
 */
public record WarehouseAggregate2Id(UUID value) {
    public WarehouseAggregate2Id {
        if (value == null) {
            throw new IllegalArgumentException("WarehouseAggregate2Id cannot be null");
        }
    }

    public static WarehouseAggregate2Id generate() {
        return new WarehouseAggregate2Id(UUID.randomUUID());
    }

    public static WarehouseAggregate2Id of(String value) {
        return new WarehouseAggregate2Id(UUID.fromString(value));
    }
}
