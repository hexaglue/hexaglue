package com.example.enterprise.shipping.domain.exception;

/**
 * Base exception for shipping domain errors.
 */
public class ShippingDomainException extends RuntimeException {
    public ShippingDomainException(String message) {
        super(message);
    }

    public ShippingDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
