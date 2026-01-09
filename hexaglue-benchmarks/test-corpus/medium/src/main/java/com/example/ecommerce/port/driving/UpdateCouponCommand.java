package com.example.ecommerce.port.driving;

/**
 * Command for updating Coupon.
 */
public record UpdateCouponCommand(
    String name,
    String description
) {
}
