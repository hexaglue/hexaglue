package com.example.enterprise.inventory.domain.specification;

import com.example.enterprise.inventory.domain.model.*;
import java.util.function.Predicate;

/**
 * Specifications for inventory domain queries.
 */
public final class InventorySpecifications {
    private InventorySpecifications() {}

    public static Predicate<InventoryAggregate1> isActive() {
        return agg -> agg.getStatus() == InventoryStatus1.ACTIVE;
    }

    public static Predicate<InventoryAggregate1> isPending() {
        return agg -> agg.getStatus() == InventoryStatus1.PENDING;
    }

    public static Predicate<InventoryAggregate1> isCompleted() {
        return agg -> agg.getStatus() == InventoryStatus1.COMPLETED;
    }

    public static Predicate<InventoryAggregate1> hasItems() {
        return agg -> !agg.getItems().isEmpty();
    }

    public static Predicate<InventoryAggregate1> hasMinimumItems(int count) {
        return agg -> agg.getItems().size() >= count;
    }
}
