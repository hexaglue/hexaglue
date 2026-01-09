package com.example.enterprise.analytics.domain.exception;

/**
 * Exception thrown when validation fails in analytics context.
 */
public class AnalyticsValidationException extends AnalyticsDomainException {
    public AnalyticsValidationException(String message) {
        super(message);
    }
}
