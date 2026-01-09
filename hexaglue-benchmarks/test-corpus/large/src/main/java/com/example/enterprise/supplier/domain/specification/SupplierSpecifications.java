package com.example.enterprise.supplier.domain.specification;

import com.example.enterprise.supplier.domain.model.*;
import java.util.function.Predicate;

/**
 * Specifications for supplier domain queries.
 */
public final class SupplierSpecifications {
    private SupplierSpecifications() {}

    public static Predicate<SupplierAggregate1> isActive() {
        return agg -> agg.getStatus() == SupplierStatus1.ACTIVE;
    }

    public static Predicate<SupplierAggregate1> isPending() {
        return agg -> agg.getStatus() == SupplierStatus1.PENDING;
    }

    public static Predicate<SupplierAggregate1> isCompleted() {
        return agg -> agg.getStatus() == SupplierStatus1.COMPLETED;
    }

    public static Predicate<SupplierAggregate1> hasItems() {
        return agg -> !agg.getItems().isEmpty();
    }

    public static Predicate<SupplierAggregate1> hasMinimumItems(int count) {
        return agg -> agg.getItems().size() >= count;
    }
}
