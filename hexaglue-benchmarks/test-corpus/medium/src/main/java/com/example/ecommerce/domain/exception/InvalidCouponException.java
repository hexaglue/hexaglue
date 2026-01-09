package com.example.ecommerce.domain.exception;

/**
 * Exception thrown when Invalid coupon.
 */
public class InvalidCouponException extends DomainException {
    public InvalidCouponException(String message) {
        super(message);
    }
}
