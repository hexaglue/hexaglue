package com.example.enterprise.analytics.domain.specification;

import com.example.enterprise.analytics.domain.model.*;
import java.util.function.Predicate;

/**
 * Specifications for analytics domain queries.
 */
public final class AnalyticsSpecifications {
    private AnalyticsSpecifications() {}

    public static Predicate<AnalyticsAggregate1> isActive() {
        return agg -> agg.getStatus() == AnalyticsStatus1.ACTIVE;
    }

    public static Predicate<AnalyticsAggregate1> isPending() {
        return agg -> agg.getStatus() == AnalyticsStatus1.PENDING;
    }

    public static Predicate<AnalyticsAggregate1> isCompleted() {
        return agg -> agg.getStatus() == AnalyticsStatus1.COMPLETED;
    }

    public static Predicate<AnalyticsAggregate1> hasItems() {
        return agg -> !agg.getItems().isEmpty();
    }

    public static Predicate<AnalyticsAggregate1> hasMinimumItems(int count) {
        return agg -> agg.getItems().size() >= count;
    }
}
