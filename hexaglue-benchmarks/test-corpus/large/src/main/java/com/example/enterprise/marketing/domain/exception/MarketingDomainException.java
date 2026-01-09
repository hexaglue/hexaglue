package com.example.enterprise.marketing.domain.exception;

/**
 * Base exception for marketing domain errors.
 */
public class MarketingDomainException extends RuntimeException {
    public MarketingDomainException(String message) {
        super(message);
    }

    public MarketingDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
