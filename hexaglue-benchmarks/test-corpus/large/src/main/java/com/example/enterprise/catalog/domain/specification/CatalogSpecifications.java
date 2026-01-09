package com.example.enterprise.catalog.domain.specification;

import com.example.enterprise.catalog.domain.model.*;
import java.util.function.Predicate;

/**
 * Specifications for catalog domain queries.
 */
public final class CatalogSpecifications {
    private CatalogSpecifications() {}

    public static Predicate<CatalogAggregate1> isActive() {
        return agg -> agg.getStatus() == CatalogStatus1.ACTIVE;
    }

    public static Predicate<CatalogAggregate1> isPending() {
        return agg -> agg.getStatus() == CatalogStatus1.PENDING;
    }

    public static Predicate<CatalogAggregate1> isCompleted() {
        return agg -> agg.getStatus() == CatalogStatus1.COMPLETED;
    }

    public static Predicate<CatalogAggregate1> hasItems() {
        return agg -> !agg.getItems().isEmpty();
    }

    public static Predicate<CatalogAggregate1> hasMinimumItems(int count) {
        return agg -> agg.getItems().size() >= count;
    }
}
