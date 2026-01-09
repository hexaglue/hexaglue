package com.example.enterprise.ordering.domain.specification;

import com.example.enterprise.ordering.domain.model.*;
import java.util.function.Predicate;

/**
 * Specifications for ordering domain queries.
 */
public final class OrderingSpecifications {
    private OrderingSpecifications() {}

    public static Predicate<OrderingAggregate1> isActive() {
        return agg -> agg.getStatus() == OrderingStatus1.ACTIVE;
    }

    public static Predicate<OrderingAggregate1> isPending() {
        return agg -> agg.getStatus() == OrderingStatus1.PENDING;
    }

    public static Predicate<OrderingAggregate1> isCompleted() {
        return agg -> agg.getStatus() == OrderingStatus1.COMPLETED;
    }

    public static Predicate<OrderingAggregate1> hasItems() {
        return agg -> !agg.getItems().isEmpty();
    }

    public static Predicate<OrderingAggregate1> hasMinimumItems(int count) {
        return agg -> agg.getItems().size() >= count;
    }
}
