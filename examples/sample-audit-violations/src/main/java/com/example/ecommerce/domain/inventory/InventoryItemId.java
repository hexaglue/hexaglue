package com.example.ecommerce.domain.inventory;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifier value object for the {@link InventoryItem} aggregate root.
 *
 * <p>Provides a strongly-typed identity for inventory items, ensuring that
 * inventory-related operations cannot accidentally use product or order identifiers.
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
