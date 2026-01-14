package com.example.domain;

import java.util.UUID;

/**
 * Unique identifier for a Customer.
 * Immutable value object (record).
 */
public record CustomerId(UUID value) {
    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID());
    }
}
