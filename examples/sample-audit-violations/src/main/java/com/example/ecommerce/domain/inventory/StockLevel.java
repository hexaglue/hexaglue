package com.example.ecommerce.domain.inventory;

import java.util.Objects;

/**
 * Immutable value object encapsulating the stock level state of an inventory item.
 *
 * <p>StockLevel tracks four dimensions of inventory: available quantity (ready for sale),
 * reserved quantity (held for pending orders), reorder point (threshold that triggers
 * replenishment), and reorder quantity (amount to order when replenishing).
 *
 * <p>All mutation operations return a new StockLevel instance, preserving immutability.
 * This enables safe concurrent reads and simplifies event-sourced state reconstruction.
 */
public record StockLevel(
        int available,
        int reserved,
        int reorderPoint,
        int reorderQuantity
) {

    public StockLevel {
        if (available < 0) {
            throw new IllegalArgumentException("Available stock cannot be negative");
        }
        if (reserved < 0) {
            throw new IllegalArgumentException("Reserved stock cannot be negative");
        }
        if (reorderPoint < 0) {
            throw new IllegalArgumentException("Reorder point cannot be negative");
        }
        if (reorderQuantity <= 0) {
            throw new IllegalArgumentException("Reorder quantity must be positive");
        }
    }

    public static StockLevel initial(int available, int reorderPoint, int reorderQuantity) {
        return new StockLevel(available, 0, reorderPoint, reorderQuantity);
    }

    public int totalQuantity() {
        return available + reserved;
    }

    public int freeQuantity() {
        return available;
    }

    public boolean needsReorder() {
        return available <= reorderPoint;
    }

    public boolean canReserve(int quantity) {
        return available >= quantity;
    }

    public StockLevel reserve(int quantity) {
        if (!canReserve(quantity)) {
            throw new IllegalStateException("Cannot reserve " + quantity + " items, only " + available + " available");
        }
        return new StockLevel(available - quantity, reserved + quantity, reorderPoint, reorderQuantity);
    }

    public StockLevel release(int quantity) {
        if (quantity > reserved) {
            throw new IllegalStateException("Cannot release " + quantity + " items, only " + reserved + " reserved");
        }
        return new StockLevel(available + quantity, reserved - quantity, reorderPoint, reorderQuantity);
    }

    public StockLevel confirmReservation(int quantity) {
        if (quantity > reserved) {
            throw new IllegalStateException("Cannot confirm " + quantity + " items, only " + reserved + " reserved");
        }
        return new StockLevel(available, reserved - quantity, reorderPoint, reorderQuantity);
    }

    public StockLevel replenish(int quantity) {
        return new StockLevel(available + quantity, reserved, reorderPoint, reorderQuantity);
    }

    @Override
    public String toString() {
        return String.format("StockLevel[available=%d, reserved=%d, reorderPoint=%d]",
                available, reserved, reorderPoint);
    }
}
