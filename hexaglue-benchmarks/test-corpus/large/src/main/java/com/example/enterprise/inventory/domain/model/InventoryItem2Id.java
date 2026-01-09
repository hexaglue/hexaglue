package com.example.enterprise.inventory.domain.model;

import java.util.UUID;

public record InventoryItem2Id(UUID value) {
    public InventoryItem2Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static InventoryItem2Id generate() {
        return new InventoryItem2Id(UUID.randomUUID());
    }
}
