package com.example.enterprise.analytics.domain.exception;

import com.example.enterprise.analytics.domain.model.AnalyticsAggregate2Id;

/**
 * Exception thrown when a AnalyticsAggregate2 is not found.
 */
public class AnalyticsAggregate2NotFoundException extends AnalyticsDomainException {
    public AnalyticsAggregate2NotFoundException(AnalyticsAggregate2Id id) {
        super("AnalyticsAggregate2 not found with id: " + id.value());
    }
}
