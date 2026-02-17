package com.example.ports.out;

import com.example.domain.inventory.InventoryItem;
import com.example.domain.inventory.InventoryItemId;
import java.util.Optional;

/** Secondary port for inventory persistence. */
public interface InventoryRepository {
    InventoryItem save(InventoryItem item);
    Optional<InventoryItem> findById(InventoryItemId id);
}
