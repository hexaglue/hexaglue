package com.example.enterprise.ordering.domain.model;

import java.util.UUID;

public record OrderingItem1Id(UUID value) {
    public OrderingItem1Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static OrderingItem1Id generate() {
        return new OrderingItem1Id(UUID.randomUUID());
    }
}
