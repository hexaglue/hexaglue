package com.example.domain.inventory;

import java.util.UUID;

/** InventoryItem identifier. */
public record InventoryItemId(UUID value) {
    public static InventoryItemId generate() { return new InventoryItemId(UUID.randomUUID()); }
}
