package com.example.ecommerce.domain.model;

import java.math.BigDecimal;

/**
 * Value Object representing physical dimensions in centimeters.
 */
public record Dimensions(BigDecimal length, BigDecimal width, BigDecimal height) {
    public Dimensions {
        if (length == null || length.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Length must be positive");
        }
        if (width == null || width.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Width must be positive");
        }
        if (height == null || height.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Height must be positive");
        }
    }

    public static Dimensions of(double length, double width, double height) {
        return new Dimensions(
            BigDecimal.valueOf(length),
            BigDecimal.valueOf(width),
            BigDecimal.valueOf(height)
        );
    }

    public BigDecimal volume() {
        return length.multiply(width).multiply(height);
    }
}
