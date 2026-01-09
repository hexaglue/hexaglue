package com.example.ecommerce.domain.model;

import java.time.Instant;

/**
 * Entity representing an inventory transaction.
 */
public class InventoryTransaction {
    private final InventoryTransactionType type;
    private final Quantity quantity;
    private final Instant timestamp;

    public InventoryTransaction(InventoryTransactionType type, Quantity quantity, Instant timestamp) {
        if (type == null) {
            throw new IllegalArgumentException("Transaction type cannot be null");
        }
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity cannot be null");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }

        this.type = type;
        this.quantity = quantity;
        this.timestamp = timestamp;
    }

    public InventoryTransactionType getType() {
        return type;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
