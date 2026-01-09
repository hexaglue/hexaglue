package com.example.ecommerce.domain.model;

import java.util.UUID;

/**
 * Value Object representing a ReturnId identifier.
 */
public record ReturnId(UUID value) {
    public ReturnId {
        if (value == null) {
            throw new IllegalArgumentException("ReturnId cannot be null");
        }
    }

    public static ReturnId generate() {
        return new ReturnId(UUID.randomUUID());
    }

    public static ReturnId of(String value) {
        return new ReturnId(UUID.fromString(value));
    }
}
