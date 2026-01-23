package com.regression.domain.shared;

/**
 * Simple wrapper Value Object for quantity.
 * <p>
 * Tests M13: This is a single-value record that should NOT generate
 * a QuantityEmbeddable. Instead, fields of this type should be
 * "unwrapped" to their primitive type (int) in JPA entities.
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

    public static Quantity zero() {
        return new Quantity(0);
    }

    public Quantity add(Quantity other) {
        return new Quantity(this.value + other.value);
    }

    public Quantity subtract(Quantity other) {
        return new Quantity(this.value - other.value);
    }

    public boolean isZero() {
        return value == 0;
    }
}
