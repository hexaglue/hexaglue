package com.example.domain.order;

import java.util.UUID;

/** Order identifier. */
public record OrderId(UUID value) {
    public static OrderId generate() { return new OrderId(UUID.randomUUID()); }
}
