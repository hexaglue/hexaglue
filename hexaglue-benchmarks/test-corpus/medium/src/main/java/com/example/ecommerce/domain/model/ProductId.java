package com.example.ecommerce.domain.model;

import java.util.UUID;

/**
 * Value Object representing a Product identifier.
 */
public record ProductId(UUID value) {
    public ProductId {
        if (value == null) {
            throw new IllegalArgumentException("ProductId cannot be null");
        }
    }

    public static ProductId generate() {
        return new ProductId(UUID.randomUUID());
    }

    public static ProductId of(String value) {
        return new ProductId(UUID.fromString(value));
    }
}
