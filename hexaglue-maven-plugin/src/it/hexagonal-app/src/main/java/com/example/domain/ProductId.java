package com.example.domain;

import java.util.UUID;

/**
 * Product identifier - should be classified as IDENTIFIER.
 */
public record ProductId(UUID value) {

    public static ProductId generate() {
        return new ProductId(UUID.randomUUID());
    }

    public static ProductId of(String value) {
        return new ProductId(UUID.fromString(value));
    }
}
