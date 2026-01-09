package com.example.ecommerce.domain.model;

import java.util.UUID;

/**
 * Value Object representing a CartId identifier.
 */
public record CartId(UUID value) {
    public CartId {
        if (value == null) {
            throw new IllegalArgumentException("CartId cannot be null");
        }
    }

    public static CartId generate() {
        return new CartId(UUID.randomUUID());
    }

    public static CartId of(String value) {
        return new CartId(UUID.fromString(value));
    }
}
