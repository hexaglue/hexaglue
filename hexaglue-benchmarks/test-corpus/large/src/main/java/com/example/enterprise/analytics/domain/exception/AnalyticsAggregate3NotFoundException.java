package com.example.enterprise.analytics.domain.exception;

import com.example.enterprise.analytics.domain.model.AnalyticsAggregate3Id;

/**
 * Exception thrown when a AnalyticsAggregate3 is not found.
 */
public class AnalyticsAggregate3NotFoundException extends AnalyticsDomainException {
    public AnalyticsAggregate3NotFoundException(AnalyticsAggregate3Id id) {
        super("AnalyticsAggregate3 not found with id: " + id.value());
    }
}
