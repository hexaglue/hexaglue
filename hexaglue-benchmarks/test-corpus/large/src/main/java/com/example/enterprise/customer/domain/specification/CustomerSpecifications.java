package com.example.enterprise.customer.domain.specification;

import com.example.enterprise.customer.domain.model.*;
import java.util.function.Predicate;

/**
 * Specifications for customer domain queries.
 */
public final class CustomerSpecifications {
    private CustomerSpecifications() {}

    public static Predicate<CustomerAggregate1> isActive() {
        return agg -> agg.getStatus() == CustomerStatus1.ACTIVE;
    }

    public static Predicate<CustomerAggregate1> isPending() {
        return agg -> agg.getStatus() == CustomerStatus1.PENDING;
    }

    public static Predicate<CustomerAggregate1> isCompleted() {
        return agg -> agg.getStatus() == CustomerStatus1.COMPLETED;
    }

    public static Predicate<CustomerAggregate1> hasItems() {
        return agg -> !agg.getItems().isEmpty();
    }

    public static Predicate<CustomerAggregate1> hasMinimumItems(int count) {
        return agg -> agg.getItems().size() >= count;
    }
}
