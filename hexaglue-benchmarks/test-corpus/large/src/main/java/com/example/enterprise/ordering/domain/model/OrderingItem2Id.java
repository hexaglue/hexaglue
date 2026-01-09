package com.example.enterprise.ordering.domain.model;

import java.util.UUID;

public record OrderingItem2Id(UUID value) {
    public OrderingItem2Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static OrderingItem2Id generate() {
        return new OrderingItem2Id(UUID.randomUUID());
    }
}
