package com.example.ecommerce.domain.model;

import java.util.UUID;

/**
 * Value Object representing a Customer identifier.
 */
public record CustomerId(UUID value) {
    public CustomerId {
        if (value == null) {
            throw new IllegalArgumentException("CustomerId cannot be null");
        }
    }

    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID());
    }

    public static CustomerId of(String value) {
        return new CustomerId(UUID.fromString(value));
    }
}
