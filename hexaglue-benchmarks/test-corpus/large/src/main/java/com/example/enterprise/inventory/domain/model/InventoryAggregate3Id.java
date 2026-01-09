package com.example.enterprise.inventory.domain.model;

import java.util.UUID;

/**
 * Value Object representing a InventoryAggregate3 identifier.
 */
public record InventoryAggregate3Id(UUID value) {
    public InventoryAggregate3Id {
        if (value == null) {
            throw new IllegalArgumentException("InventoryAggregate3Id cannot be null");
        }
    }

    public static InventoryAggregate3Id generate() {
        return new InventoryAggregate3Id(UUID.randomUUID());
    }

    public static InventoryAggregate3Id of(String value) {
        return new InventoryAggregate3Id(UUID.fromString(value));
    }
}
