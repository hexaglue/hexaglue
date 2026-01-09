package com.example.enterprise.warehouse.domain.model;

import java.util.UUID;

/**
 * Value Object representing a WarehouseAggregate3 identifier.
 */
public record WarehouseAggregate3Id(UUID value) {
    public WarehouseAggregate3Id {
        if (value == null) {
            throw new IllegalArgumentException("WarehouseAggregate3Id cannot be null");
        }
    }

    public static WarehouseAggregate3Id generate() {
        return new WarehouseAggregate3Id(UUID.randomUUID());
    }

    public static WarehouseAggregate3Id of(String value) {
        return new WarehouseAggregate3Id(UUID.fromString(value));
    }
}
