package com.example.enterprise.shipping.domain.specification;

import com.example.enterprise.shipping.domain.model.*;
import java.util.function.Predicate;

/**
 * Specifications for shipping domain queries.
 */
public final class ShippingSpecifications {
    private ShippingSpecifications() {}

    public static Predicate<ShippingAggregate1> isActive() {
        return agg -> agg.getStatus() == ShippingStatus1.ACTIVE;
    }

    public static Predicate<ShippingAggregate1> isPending() {
        return agg -> agg.getStatus() == ShippingStatus1.PENDING;
    }

    public static Predicate<ShippingAggregate1> isCompleted() {
        return agg -> agg.getStatus() == ShippingStatus1.COMPLETED;
    }

    public static Predicate<ShippingAggregate1> hasItems() {
        return agg -> !agg.getItems().isEmpty();
    }

    public static Predicate<ShippingAggregate1> hasMinimumItems(int count) {
        return agg -> agg.getItems().size() >= count;
    }
}
