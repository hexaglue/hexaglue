package com.example.enterprise.payment.domain.specification;

import com.example.enterprise.payment.domain.model.*;
import java.util.function.Predicate;

/**
 * Specifications for payment domain queries.
 */
public final class PaymentSpecifications {
    private PaymentSpecifications() {}

    public static Predicate<PaymentAggregate1> isActive() {
        return agg -> agg.getStatus() == PaymentStatus1.ACTIVE;
    }

    public static Predicate<PaymentAggregate1> isPending() {
        return agg -> agg.getStatus() == PaymentStatus1.PENDING;
    }

    public static Predicate<PaymentAggregate1> isCompleted() {
        return agg -> agg.getStatus() == PaymentStatus1.COMPLETED;
    }

    public static Predicate<PaymentAggregate1> hasItems() {
        return agg -> !agg.getItems().isEmpty();
    }

    public static Predicate<PaymentAggregate1> hasMinimumItems(int count) {
        return agg -> agg.getItems().size() >= count;
    }
}
