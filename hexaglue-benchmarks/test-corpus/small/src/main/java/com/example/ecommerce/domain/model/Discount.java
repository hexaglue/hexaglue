package com.example.ecommerce.domain.model;

import java.math.BigDecimal;

/**
 * Value Object representing a discount.
 */
public record Discount(BigDecimal percentage) {
    public Discount {
        if (percentage == null || percentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Discount percentage cannot be null or negative");
        }
        if (percentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Discount percentage cannot exceed 100%");
        }
    }

    public static Discount of(double percentage) {
        return new Discount(BigDecimal.valueOf(percentage));
    }

    public Money apply(Money price) {
        BigDecimal discountAmount = price.amount()
            .multiply(percentage)
            .divide(new BigDecimal("100"), price.amount().scale(), BigDecimal.ROUND_HALF_UP);
        return new Money(price.amount().subtract(discountAmount), price.currency());
    }

    public boolean isZero() {
        return percentage.compareTo(BigDecimal.ZERO) == 0;
    }
}
