package com.example.domain;

import java.util.UUID;

/**
 * Unique identifier for a Product.
 */
public record ProductId(UUID value) {
    public static ProductId generate() {
        return new ProductId(UUID.randomUUID());
    }
}
