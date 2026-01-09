package com.example.enterprise.marketing.domain.exception;

/**
 * Exception thrown when validation fails in marketing context.
 */
public class MarketingValidationException extends MarketingDomainException {
    public MarketingValidationException(String message) {
        super(message);
    }
}
