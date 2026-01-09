package com.example.ecommerce.port.driving;

/**
 * Command for creating Coupon.
 */
public record CreateCouponCommand(
    String name,
    String description
) {
}
