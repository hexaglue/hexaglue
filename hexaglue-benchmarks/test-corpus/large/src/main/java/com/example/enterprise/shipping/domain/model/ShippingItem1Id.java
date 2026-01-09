package com.example.enterprise.shipping.domain.model;

import java.util.UUID;

public record ShippingItem1Id(UUID value) {
    public ShippingItem1Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static ShippingItem1Id generate() {
        return new ShippingItem1Id(UUID.randomUUID());
    }
}
