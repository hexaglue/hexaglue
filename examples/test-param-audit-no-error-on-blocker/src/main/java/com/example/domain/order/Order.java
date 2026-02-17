package com.example.domain.order;

import com.example.domain.inventory.InventoryItem;
import java.time.Instant;

/** Order aggregate root. Has a direct reference to InventoryItem (another aggregate) creating a BLOCKER cycle. */
public class Order {
    private final OrderId id;
    private final Instant createdAt;
    private InventoryItem reservedInventory;

    public Order(OrderId id) {
        this.id = id;
        this.createdAt = Instant.now();
    }

    public OrderId getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public InventoryItem getReservedInventory() { return reservedInventory; }
    public void setReservedInventory(InventoryItem inventory) { this.reservedInventory = inventory; }
}
