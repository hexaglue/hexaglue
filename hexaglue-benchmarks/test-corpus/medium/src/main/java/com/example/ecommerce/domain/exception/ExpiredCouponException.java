package com.example.ecommerce.domain.exception;

/**
 * Exception thrown when Expired coupon.
 */
public class ExpiredCouponException extends DomainException {
    public ExpiredCouponException(String message) {
        super(message);
    }
}
