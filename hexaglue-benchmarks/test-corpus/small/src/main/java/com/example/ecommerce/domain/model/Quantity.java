package com.example.ecommerce.domain.model;

/**
 * Value Object representing a quantity.
 */
public record Quantity(int value) {
    public Quantity {
        if (value < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
    }

    public static Quantity zero() {
        return new Quantity(0);
    }

    public Quantity add(Quantity other) {
        return new Quantity(this.value + other.value);
    }

    public Quantity subtract(Quantity other) {
        int result = this.value - other.value;
        if (result < 0) {
            throw new IllegalArgumentException("Cannot subtract to negative quantity");
        }
        return new Quantity(result);
    }

    public boolean isZero() {
        return value == 0;
    }

    public boolean isGreaterThan(Quantity other) {
        return this.value > other.value;
    }
}
