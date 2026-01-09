package com.example.enterprise.supplier.domain.model;

import java.time.Instant;

/**
 * Value Object representing a timestamp in supplier context.
 */
public record SupplierTimestamp(Instant value) {
    public SupplierTimestamp {
        if (value == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
    }

    public static SupplierTimestamp now() {
        return new SupplierTimestamp(Instant.now());
    }

    public boolean isBefore(SupplierTimestamp other) {
        return this.value.isBefore(other.value);
    }

    public boolean isAfter(SupplierTimestamp other) {
        return this.value.isAfter(other.value);
    }
}
