package com.example.ecommerce.domain.product;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing the identity of a Product.
 */
public record ProductId(UUID value) {

    public ProductId {
        Objects.requireNonNull(value, "Product ID cannot be null");
    }

    public static ProductId generate() {
        return new ProductId(UUID.randomUUID());
    }

    public static ProductId from(String value) {
        return new ProductId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
