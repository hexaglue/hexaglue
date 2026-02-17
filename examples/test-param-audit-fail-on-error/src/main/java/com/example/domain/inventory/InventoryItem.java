package com.example.domain.inventory;

import com.example.domain.order.Order;

/** InventoryItem aggregate root. Has a direct reference to Order (another aggregate) creating a BLOCKER cycle. */
public class InventoryItem {
    private final InventoryItemId id;
    private StockLevel stockLevel;
    private Order reservedForOrder;

    public InventoryItem(InventoryItemId id, StockLevel stockLevel) {
        this.id = id;
        this.stockLevel = stockLevel;
    }

    public InventoryItemId getId() { return id; }
    public StockLevel getStockLevel() { return stockLevel; }
    public Order getReservedForOrder() { return reservedForOrder; }
    public void setReservedForOrder(Order order) { this.reservedForOrder = order; }
}
