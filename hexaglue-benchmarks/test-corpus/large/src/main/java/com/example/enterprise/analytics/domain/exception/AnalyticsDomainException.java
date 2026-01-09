package com.example.enterprise.analytics.domain.exception;

/**
 * Base exception for analytics domain errors.
 */
public class AnalyticsDomainException extends RuntimeException {
    public AnalyticsDomainException(String message) {
        super(message);
    }

    public AnalyticsDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
