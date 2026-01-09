package com.example.enterprise.shipping.domain.model;

import java.util.UUID;

public record ShippingItem2Id(UUID value) {
    public ShippingItem2Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static ShippingItem2Id generate() {
        return new ShippingItem2Id(UUID.randomUUID());
    }
}
