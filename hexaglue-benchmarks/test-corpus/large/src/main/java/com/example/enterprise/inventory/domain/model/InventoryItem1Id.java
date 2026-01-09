package com.example.enterprise.inventory.domain.model;

import java.util.UUID;

public record InventoryItem1Id(UUID value) {
    public InventoryItem1Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static InventoryItem1Id generate() {
        return new InventoryItem1Id(UUID.randomUUID());
    }
}
