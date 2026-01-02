package com.ecommerce.domain.order;

/**
 * Quantity value object ensuring non-negative values.
 */
public record Quantity(int value) {

    public Quantity {
        if (value < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }

    public Quantity add(Quantity other) {
        return new Quantity(value + other.value);
    }

    public boolean isZero() {
        return value == 0;
    }
}
