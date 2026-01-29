package com.example.ecommerce.infrastructure.persistence;

import com.example.ecommerce.application.port.driven.InventoryRepository;
import com.example.ecommerce.domain.inventory.InventoryItem;
import com.example.ecommerce.domain.inventory.InventoryItemId;
import com.example.ecommerce.domain.product.ProductId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * In-memory driven adapter implementing the {@link InventoryRepository} port.
 *
 * <p>This adapter stores inventory items in a HashMap with a secondary index
 * on ProductId for efficient product-based lookups. It is suitable for testing
 * and demonstration purposes where database persistence is not required.
 */
public class InMemoryInventoryRepository implements InventoryRepository {

    private final Map<InventoryItemId, InventoryItem> storage = new HashMap<>();
    private final Map<ProductId, InventoryItemId> productIndex = new HashMap<>();

    @Override
    public void save(InventoryItem item) {
        storage.put(item.getId(), item);
        productIndex.put(item.getProductId(), item.getId());
    }

    @Override
    public Optional<InventoryItem> findById(InventoryItemId id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<InventoryItem> findByProductId(ProductId productId) {
        InventoryItemId itemId = productIndex.get(productId);
        if (itemId == null) {
            return Optional.empty();
        }
        return findById(itemId);
    }

    @Override
    public List<InventoryItem> findItemsNeedingReorder() {
        return storage.values().stream()
                .filter(InventoryItem::needsReorder)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(InventoryItemId id) {
        InventoryItem item = storage.remove(id);
        if (item != null) {
            productIndex.remove(item.getProductId());
        }
    }
}
