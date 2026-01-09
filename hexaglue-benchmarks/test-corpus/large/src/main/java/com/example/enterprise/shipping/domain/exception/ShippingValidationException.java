package com.example.enterprise.shipping.domain.exception;

/**
 * Exception thrown when validation fails in shipping context.
 */
public class ShippingValidationException extends ShippingDomainException {
    public ShippingValidationException(String message) {
        super(message);
    }
}
