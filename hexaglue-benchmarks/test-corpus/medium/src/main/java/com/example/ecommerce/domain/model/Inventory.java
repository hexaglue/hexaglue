package com.example.ecommerce.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root representing inventory for a product.
 */
public class Inventory {
    private final InventoryId id;
    private final ProductId productId;
    private Quantity availableQuantity;
    private Quantity reservedQuantity;
    private final List<InventoryTransaction> transactions;
    private final Instant createdAt;
    private Instant updatedAt;

    public Inventory(InventoryId id, ProductId productId, Quantity initialQuantity) {
        if (id == null) {
            throw new IllegalArgumentException("InventoryId cannot be null");
        }
        if (productId == null) {
            throw new IllegalArgumentException("ProductId cannot be null");
        }
        if (initialQuantity == null) {
            throw new IllegalArgumentException("Initial quantity cannot be null");
        }

        this.id = id;
        this.productId = productId;
        this.availableQuantity = initialQuantity;
        this.reservedQuantity = Quantity.zero();
        this.transactions = new ArrayList<>();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void reserve(Quantity quantity) {
        if (availableQuantity.value() < quantity.value()) {
            throw new IllegalStateException("Insufficient inventory to reserve");
        }
        this.availableQuantity = availableQuantity.subtract(quantity);
        this.reservedQuantity = reservedQuantity.add(quantity);
        this.updatedAt = Instant.now();
    }

    public void release(Quantity quantity) {
        if (reservedQuantity.value() < quantity.value()) {
            throw new IllegalStateException("Cannot release more than reserved");
        }
        this.reservedQuantity = reservedQuantity.subtract(quantity);
        this.availableQuantity = availableQuantity.add(quantity);
        this.updatedAt = Instant.now();
    }

    public void addStock(Quantity quantity) {
        this.availableQuantity = availableQuantity.add(quantity);
        this.transactions.add(new InventoryTransaction(
            InventoryTransactionType.STOCK_IN,
            quantity,
            Instant.now()
        ));
        this.updatedAt = Instant.now();
    }

    public void removeStock(Quantity quantity) {
        this.availableQuantity = availableQuantity.subtract(quantity);
        this.transactions.add(new InventoryTransaction(
            InventoryTransactionType.STOCK_OUT,
            quantity,
            Instant.now()
        ));
        this.updatedAt = Instant.now();
    }

    public boolean isAvailable(Quantity quantity) {
        return availableQuantity.isGreaterThan(quantity) || availableQuantity.value() == quantity.value();
    }

    public InventoryId getId() {
        return id;
    }

    public ProductId getProductId() {
        return productId;
    }

    public Quantity getAvailableQuantity() {
        return availableQuantity;
    }

    public Quantity getReservedQuantity() {
        return reservedQuantity;
    }

    public List<InventoryTransaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
