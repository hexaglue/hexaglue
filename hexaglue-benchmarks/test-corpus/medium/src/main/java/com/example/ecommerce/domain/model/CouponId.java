package com.example.ecommerce.domain.model;

import java.util.UUID;

/**
 * Value Object representing a CouponId identifier.
 */
public record CouponId(UUID value) {
    public CouponId {
        if (value == null) {
            throw new IllegalArgumentException("CouponId cannot be null");
        }
    }

    public static CouponId generate() {
        return new CouponId(UUID.randomUUID());
    }

    public static CouponId of(String value) {
        return new CouponId(UUID.fromString(value));
    }
}
