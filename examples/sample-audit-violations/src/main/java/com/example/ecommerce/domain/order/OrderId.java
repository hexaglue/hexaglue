package com.example.ecommerce.domain.order;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing the identity of an Order.
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
