package com.example.ecommerce.domain.model;

import java.util.UUID;

/**
 * Value Object representing a WarehouseId identifier.
 */
public record WarehouseId(UUID value) {
    public WarehouseId {
        if (value == null) {
            throw new IllegalArgumentException("WarehouseId cannot be null");
        }
    }

    public static WarehouseId generate() {
        return new WarehouseId(UUID.randomUUID());
    }

    public static WarehouseId of(String value) {
        return new WarehouseId(UUID.fromString(value));
    }
}
