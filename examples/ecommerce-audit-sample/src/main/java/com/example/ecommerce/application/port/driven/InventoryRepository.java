package com.example.ecommerce.application.port.driven;

import com.example.ecommerce.domain.inventory.InventoryItem;
import com.example.ecommerce.domain.inventory.InventoryItemId;
import com.example.ecommerce.domain.product.ProductId;

import java.util.List;
import java.util.Optional;

/**
 * Driven port (repository interface) for InventoryItem persistence.
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
