package com.example.ecommerce.domain.order;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifier value object for the {@link Order} aggregate root.
 *
 * <p>Wraps a UUID to provide a strongly-typed identity, preventing confusion
 * between order identifiers and other UUID-based identifiers in the system
 * such as {@link com.example.ecommerce.domain.customer.CustomerId} or
 * {@link com.example.ecommerce.domain.product.ProductId}.
 */
public record OrderId(UUID value) {

    public OrderId {
        Objects.requireNonNull(value, "Order ID cannot be null");
    }

    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }

    public static OrderId from(String value) {
        return new OrderId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
