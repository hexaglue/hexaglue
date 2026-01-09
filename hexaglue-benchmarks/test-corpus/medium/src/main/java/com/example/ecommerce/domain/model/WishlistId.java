package com.example.ecommerce.domain.model;

import java.util.UUID;

/**
 * Value Object representing a WishlistId identifier.
 */
public record WishlistId(UUID value) {
    public WishlistId {
        if (value == null) {
            throw new IllegalArgumentException("WishlistId cannot be null");
        }
    }

    public static WishlistId generate() {
        return new WishlistId(UUID.randomUUID());
    }

    public static WishlistId of(String value) {
        return new WishlistId(UUID.fromString(value));
    }
}
