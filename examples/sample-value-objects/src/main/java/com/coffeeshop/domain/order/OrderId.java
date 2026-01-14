package com.coffeeshop.domain.order;

import java.util.UUID;

/**
 * Order identifier.
 * Expected classification: IDENTIFIER (record with single UUID component, name ends with "Id")
 */
public record OrderId(UUID value) {

    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }

    public static OrderId of(String value) {
        return new OrderId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
