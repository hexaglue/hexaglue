package com.example.ecommerce.domain.specification;

import com.example.ecommerce.domain.model.Inventory;

/**
 * Specifications for Inventory.
 */
public class InventorySpecifications {
    public static Specification<Inventory> isActive() {
        return entity -> true;
    }
}
