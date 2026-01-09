package com.example.ecommerce.domain.model;

/**
 * Value Object representing a coupon code.
 */
public record CouponCode(String value) {
    public CouponCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CouponCode cannot be null or blank");
        }
    }
}
