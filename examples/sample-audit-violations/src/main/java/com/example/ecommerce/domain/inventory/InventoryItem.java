package com.example.ecommerce.domain.inventory;

import com.example.ecommerce.domain.shared.AggregateRoot;
import com.example.ecommerce.domain.product.ProductId;
import com.example.ecommerce.domain.order.Order;

import java.time.Instant;
import java.util.Objects;

/**
 * InventoryItem aggregate root for managing stock levels.
 *
 * AUDIT VIOLATION: ddd:aggregate-cycle
 * This aggregate has a direct reference to Order aggregate,
 * which creates a bidirectional dependency between aggregates.
 */
public class InventoryItem extends AggregateRoot<InventoryItemId> {

    private final InventoryItemId id;
    private final ProductId productId;
    private StockLevel stockLevel;
    private String warehouseLocation;
    private final Instant createdAt;
    private Instant updatedAt;

    // VIOLATION: Direct reference to another aggregate
    private Order reservedForOrder;

    private InventoryItem(InventoryItemId id, ProductId productId, StockLevel stockLevel) {
        this.id = Objects.requireNonNull(id, "Inventory Item ID cannot be null");
        this.productId = Objects.requireNonNull(productId, "Product ID cannot be null");
        this.stockLevel = Objects.requireNonNull(stockLevel, "Stock level cannot be null");
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public static InventoryItem create(ProductId productId, int initialQuantity,
                                       int reorderPoint, int reorderQuantity) {
        StockLevel stockLevel = StockLevel.initial(initialQuantity, reorderPoint, reorderQuantity);
        return new InventoryItem(InventoryItemId.generate(), productId, stockLevel);
    }

    @Override
    public InventoryItemId getId() {
        return id;
    }

    public ProductId getProductId() {
        return productId;
    }

    public StockLevel getStockLevel() {
        return stockLevel;
    }

    public String getWarehouseLocation() {
        return warehouseLocation;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * VIOLATION: Returns reference to another aggregate
     */
    public Order getReservedForOrder() {
        return reservedForOrder;
    }

    /**
     * VIOLATION: Sets reference to another aggregate
     */
    public void setReservedForOrder(Order order) {
        this.reservedForOrder = order;
    }

    public void setWarehouseLocation(String location) {
        this.warehouseLocation = location;
        this.updatedAt = Instant.now();
    }

    public boolean canReserve(int quantity) {
        return stockLevel.canReserve(quantity);
    }

    public void reserve(int quantity) {
        this.stockLevel = stockLevel.reserve(quantity);
        this.updatedAt = Instant.now();
    }

    public void release(int quantity) {
        this.stockLevel = stockLevel.release(quantity);
        this.updatedAt = Instant.now();
    }

    public void confirmReservation(int quantity) {
        this.stockLevel = stockLevel.confirmReservation(quantity);
        this.updatedAt = Instant.now();
    }

    public void replenish(int quantity) {
        this.stockLevel = stockLevel.replenish(quantity);
        this.updatedAt = Instant.now();
    }

    public boolean needsReorder() {
        return stockLevel.needsReorder();
    }

    public int getAvailableQuantity() {
        return stockLevel.available();
    }

    public int getReservedQuantity() {
        return stockLevel.reserved();
    }

    public int getTotalQuantity() {
        return stockLevel.totalQuantity();
    }
}
