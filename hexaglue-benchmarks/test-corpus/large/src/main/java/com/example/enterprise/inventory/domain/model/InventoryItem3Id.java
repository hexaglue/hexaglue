package com.example.enterprise.inventory.domain.model;

import java.util.UUID;

public record InventoryItem3Id(UUID value) {
    public InventoryItem3Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static InventoryItem3Id generate() {
        return new InventoryItem3Id(UUID.randomUUID());
    }
}
