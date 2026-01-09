package com.example.enterprise.analytics.domain.exception;

import com.example.enterprise.analytics.domain.model.AnalyticsAggregate1Id;

/**
 * Exception thrown when a AnalyticsAggregate1 is not found.
 */
public class AnalyticsAggregate1NotFoundException extends AnalyticsDomainException {
    public AnalyticsAggregate1NotFoundException(AnalyticsAggregate1Id id) {
        super("AnalyticsAggregate1 not found with id: " + id.value());
    }
}
