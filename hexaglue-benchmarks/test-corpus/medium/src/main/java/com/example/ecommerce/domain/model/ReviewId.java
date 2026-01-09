package com.example.ecommerce.domain.model;

import java.util.UUID;

/**
 * Value Object representing a ReviewId identifier.
 */
public record ReviewId(UUID value) {
    public ReviewId {
        if (value == null) {
            throw new IllegalArgumentException("ReviewId cannot be null");
        }
    }

    public static ReviewId generate() {
        return new ReviewId(UUID.randomUUID());
    }

    public static ReviewId of(String value) {
        return new ReviewId(UUID.fromString(value));
    }
}
