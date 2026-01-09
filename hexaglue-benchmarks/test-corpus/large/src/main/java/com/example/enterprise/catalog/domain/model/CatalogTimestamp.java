package com.example.enterprise.catalog.domain.model;

import java.time.Instant;

/**
 * Value Object representing a timestamp in catalog context.
 */
public record CatalogTimestamp(Instant value) {
    public CatalogTimestamp {
        if (value == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
    }

    public static CatalogTimestamp now() {
        return new CatalogTimestamp(Instant.now());
    }

    public boolean isBefore(CatalogTimestamp other) {
        return this.value.isBefore(other.value);
    }

    public boolean isAfter(CatalogTimestamp other) {
        return this.value.isAfter(other.value);
    }
}
