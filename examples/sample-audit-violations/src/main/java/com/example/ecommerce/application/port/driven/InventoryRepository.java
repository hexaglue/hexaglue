package com.example.ecommerce.application.port.driven;

import com.example.ecommerce.domain.inventory.InventoryItem;
import com.example.ecommerce.domain.inventory.InventoryItemId;
import com.example.ecommerce.domain.product.ProductId;

import java.util.List;
import java.util.Optional;

/**
 * Driven port defining the persistence contract for {@link com.example.ecommerce.domain.inventory.InventoryItem} aggregates.
 *
 * <p>This repository interface provides storage and retrieval operations for inventory
 * items, including lookup by product identifier and a specialized query for items
 * that have fallen below their reorder threshold, enabling automated replenishment workflows.
 */
public interface InventoryRepository {

    /**
     * Saves an inventory item.
     */
    void save(InventoryItem item);

    /**
     * Finds an inventory item by ID.
     */
    Optional<InventoryItem> findById(InventoryItemId id);

    /**
     * Finds an inventory item by product ID.
     */
    Optional<InventoryItem> findByProductId(ProductId productId);

    /**
     * Finds all items that need reorder.
     */
    List<InventoryItem> findItemsNeedingReorder();

    /**
     * Deletes an inventory item.
     */
    void delete(InventoryItemId id);
}
