package com.example.enterprise.warehouse.domain.model;

import java.time.Instant;

/**
 * Value Object representing a timestamp in warehouse context.
 */
public record WarehouseTimestamp(Instant value) {
    public WarehouseTimestamp {
        if (value == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
    }

    public static WarehouseTimestamp now() {
        return new WarehouseTimestamp(Instant.now());
    }

    public boolean isBefore(WarehouseTimestamp other) {
        return this.value.isBefore(other.value);
    }

    public boolean isAfter(WarehouseTimestamp other) {
        return this.value.isAfter(other.value);
    }
}
