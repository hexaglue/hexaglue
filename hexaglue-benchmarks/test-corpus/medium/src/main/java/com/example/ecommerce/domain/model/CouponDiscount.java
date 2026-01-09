package com.example.ecommerce.domain.model;

/**
 * Value Object representing Discount from coupon.
 */
public record CouponDiscount(String value) {
    public CouponDiscount {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CouponDiscount cannot be null or blank");
        }
    }
}
