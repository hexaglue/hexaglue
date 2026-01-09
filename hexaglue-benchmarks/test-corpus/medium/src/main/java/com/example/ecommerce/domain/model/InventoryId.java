package com.example.ecommerce.domain.model;

import java.util.UUID;

/**
 * Value Object representing an Inventory identifier.
 */
public record InventoryId(UUID value) {
    public InventoryId {
        if (value == null) {
            throw new IllegalArgumentException("InventoryId cannot be null");
        }
    }

    public static InventoryId generate() {
        return new InventoryId(UUID.randomUUID());
    }

    public static InventoryId of(String value) {
        return new InventoryId(UUID.fromString(value));
    }
}
