package com.example.enterprise.ordering.domain.model;

import java.util.UUID;

public record OrderingItem3Id(UUID value) {
    public OrderingItem3Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static OrderingItem3Id generate() {
        return new OrderingItem3Id(UUID.randomUUID());
    }
}
