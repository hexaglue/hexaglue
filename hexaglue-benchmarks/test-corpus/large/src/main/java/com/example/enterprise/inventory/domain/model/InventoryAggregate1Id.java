package com.example.enterprise.inventory.domain.model;

import java.util.UUID;

/**
 * Value Object representing a InventoryAggregate1 identifier.
 */
public record InventoryAggregate1Id(UUID value) {
    public InventoryAggregate1Id {
        if (value == null) {
            throw new IllegalArgumentException("InventoryAggregate1Id cannot be null");
        }
    }

    public static InventoryAggregate1Id generate() {
        return new InventoryAggregate1Id(UUID.randomUUID());
    }

    public static InventoryAggregate1Id of(String value) {
        return new InventoryAggregate1Id(UUID.fromString(value));
    }
}
