package com.example.domain;

import java.util.UUID;

/**
 * Unique identifier for an Order.
 */
public record OrderId(UUID value) {
    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }
}
