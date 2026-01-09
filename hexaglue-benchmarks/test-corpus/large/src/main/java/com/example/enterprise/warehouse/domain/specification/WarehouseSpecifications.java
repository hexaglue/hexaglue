package com.example.enterprise.warehouse.domain.specification;

import com.example.enterprise.warehouse.domain.model.*;
import java.util.function.Predicate;

/**
 * Specifications for warehouse domain queries.
 */
public final class WarehouseSpecifications {
    private WarehouseSpecifications() {}

    public static Predicate<WarehouseAggregate1> isActive() {
        return agg -> agg.getStatus() == WarehouseStatus1.ACTIVE;
    }

    public static Predicate<WarehouseAggregate1> isPending() {
        return agg -> agg.getStatus() == WarehouseStatus1.PENDING;
    }

    public static Predicate<WarehouseAggregate1> isCompleted() {
        return agg -> agg.getStatus() == WarehouseStatus1.COMPLETED;
    }

    public static Predicate<WarehouseAggregate1> hasItems() {
        return agg -> !agg.getItems().isEmpty();
    }

    public static Predicate<WarehouseAggregate1> hasMinimumItems(int count) {
        return agg -> agg.getItems().size() >= count;
    }
}
