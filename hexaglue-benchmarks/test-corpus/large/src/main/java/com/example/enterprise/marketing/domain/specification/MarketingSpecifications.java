package com.example.enterprise.marketing.domain.specification;

import com.example.enterprise.marketing.domain.model.*;
import java.util.function.Predicate;

/**
 * Specifications for marketing domain queries.
 */
public final class MarketingSpecifications {
    private MarketingSpecifications() {}

    public static Predicate<MarketingAggregate1> isActive() {
        return agg -> agg.getStatus() == MarketingStatus1.ACTIVE;
    }

    public static Predicate<MarketingAggregate1> isPending() {
        return agg -> agg.getStatus() == MarketingStatus1.PENDING;
    }

    public static Predicate<MarketingAggregate1> isCompleted() {
        return agg -> agg.getStatus() == MarketingStatus1.COMPLETED;
    }

    public static Predicate<MarketingAggregate1> hasItems() {
        return agg -> !agg.getItems().isEmpty();
    }

    public static Predicate<MarketingAggregate1> hasMinimumItems(int count) {
        return agg -> agg.getItems().size() >= count;
    }
}
