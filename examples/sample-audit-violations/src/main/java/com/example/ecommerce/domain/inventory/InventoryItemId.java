package com.example.ecommerce.domain.inventory;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing the identity of an InventoryItem.
 */
public record InventoryItemId(UUID value) {

    public InventoryItemId {
        Objects.requireNonNull(value, "Inventory Item ID cannot be null");
    }

    public static InventoryItemId generate() {
        return new InventoryItemId(UUID.randomUUID());
    }

    public static InventoryItemId from(String value) {
        return new InventoryItemId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
