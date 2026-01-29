package com.example.ecommerce.domain.product;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifier value object for the {@link Product} aggregate root.
 *
 * <p>Wraps a UUID to provide a strongly-typed product identity. Used throughout
 * the system to reference products in orders, inventory items, and catalog operations
 * without exposing the underlying UUID representation.
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
