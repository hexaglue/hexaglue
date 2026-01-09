package com.example.enterprise.shipping.domain.model;

import java.util.UUID;

public record ShippingItem3Id(UUID value) {
    public ShippingItem3Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static ShippingItem3Id generate() {
        return new ShippingItem3Id(UUID.randomUUID());
    }
}
