package com.example.ecommerce.domain.exception;

import com.example.ecommerce.domain.model.CouponId;

/**
 * Exception thrown when Coupon is not found.
 */
public class CouponNotFoundException extends DomainException {
    private final CouponId id;

    public CouponNotFoundException(CouponId id) {
        super("Coupon not found: " + id);
        this.id = id;
    }

    public CouponId getId() {
        return id;
    }
}
