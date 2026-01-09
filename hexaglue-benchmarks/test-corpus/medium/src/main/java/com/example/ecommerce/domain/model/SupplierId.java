package com.example.ecommerce.domain.model;

import java.util.UUID;

/**
 * Value Object representing a SupplierId identifier.
 */
public record SupplierId(UUID value) {
    public SupplierId {
        if (value == null) {
            throw new IllegalArgumentException("SupplierId cannot be null");
        }
    }

    public static SupplierId generate() {
        return new SupplierId(UUID.randomUUID());
    }

    public static SupplierId of(String value) {
        return new SupplierId(UUID.fromString(value));
    }
}
