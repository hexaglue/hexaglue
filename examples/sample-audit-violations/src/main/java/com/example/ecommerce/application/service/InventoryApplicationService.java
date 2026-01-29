package com.example.ecommerce.application.service;

import com.example.ecommerce.application.port.driven.InventoryRepository;
import com.example.ecommerce.domain.inventory.InventoryItem;
import com.example.ecommerce.domain.product.ProductId;

import java.util.List;

/**
 * Application service orchestrating inventory management operations.
 *
 * <p>Coordinates stock management workflows including item creation, availability
 * checks, inventory reservation for pending orders, release of cancelled reservations,
 * reservation confirmation upon payment, and stock replenishment. Also provides
 * reorder alerting by identifying items whose stock has fallen below their reorder point.
 */
public class InventoryApplicationService {

    private final InventoryRepository inventoryRepository;

    public InventoryApplicationService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public void createInventoryItem(ProductId productId, int initialQuantity,
                                    int reorderPoint, int reorderQuantity) {
        InventoryItem item = InventoryItem.create(productId, initialQuantity,
                reorderPoint, reorderQuantity);
        inventoryRepository.save(item);
    }

    public boolean checkAvailability(ProductId productId, int quantity) {
        return inventoryRepository.findByProductId(productId)
                .map(item -> item.canReserve(quantity))
                .orElse(false);
    }

    public void reserveInventory(ProductId productId, int quantity) {
        InventoryItem item = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found for product: " + productId));

        if (!item.canReserve(quantity)) {
            throw new IllegalStateException("Insufficient inventory for product: " + productId);
        }

        item.reserve(quantity);
        inventoryRepository.save(item);
    }

    public void releaseInventory(ProductId productId, int quantity) {
        InventoryItem item = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found for product: " + productId));

        item.release(quantity);
        inventoryRepository.save(item);
    }

    public void confirmReservation(ProductId productId, int quantity) {
        InventoryItem item = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found for product: " + productId));

        item.confirmReservation(quantity);
        inventoryRepository.save(item);
    }

    public void replenishInventory(ProductId productId, int quantity) {
        InventoryItem item = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found for product: " + productId));

        item.replenish(quantity);
        inventoryRepository.save(item);
    }

    public List<InventoryItem> getItemsNeedingReorder() {
        return inventoryRepository.findItemsNeedingReorder();
    }

    public int getAvailableQuantity(ProductId productId) {
        return inventoryRepository.findByProductId(productId)
                .map(InventoryItem::getAvailableQuantity)
                .orElse(0);
    }
}
