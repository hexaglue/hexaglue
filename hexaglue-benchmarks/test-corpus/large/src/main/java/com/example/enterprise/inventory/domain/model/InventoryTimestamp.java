package com.example.enterprise.inventory.domain.model;

import java.time.Instant;

/**
 * Value Object representing a timestamp in inventory context.
 */
public record InventoryTimestamp(Instant value) {
    public InventoryTimestamp {
        if (value == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
    }

    public static InventoryTimestamp now() {
        return new InventoryTimestamp(Instant.now());
    }

    public boolean isBefore(InventoryTimestamp other) {
        return this.value.isBefore(other.value);
    }

    public boolean isAfter(InventoryTimestamp other) {
        return this.value.isAfter(other.value);
    }
}
