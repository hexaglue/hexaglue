package com.example.enterprise.inventory.domain.model;

import java.util.UUID;

/**
 * Value Object representing a InventoryAggregate2 identifier.
 */
public record InventoryAggregate2Id(UUID value) {
    public InventoryAggregate2Id {
        if (value == null) {
            throw new IllegalArgumentException("InventoryAggregate2Id cannot be null");
        }
    }

    public static InventoryAggregate2Id generate() {
        return new InventoryAggregate2Id(UUID.randomUUID());
    }

    public static InventoryAggregate2Id of(String value) {
        return new InventoryAggregate2Id(UUID.fromString(value));
    }
}
