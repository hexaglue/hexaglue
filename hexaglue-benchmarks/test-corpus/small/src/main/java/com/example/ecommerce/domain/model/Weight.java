package com.example.ecommerce.domain.model;

import java.math.BigDecimal;

/**
 * Value Object representing weight in kilograms.
 */
public record Weight(BigDecimal kilograms) {
    public Weight {
        if (kilograms == null || kilograms.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Weight cannot be null or negative");
        }
    }

    public static Weight ofKg(double kg) {
        return new Weight(BigDecimal.valueOf(kg));
    }

    public static Weight ofGrams(double grams) {
        return new Weight(BigDecimal.valueOf(grams / 1000.0));
    }

    public Weight add(Weight other) {
        return new Weight(this.kilograms.add(other.kilograms));
    }

    public BigDecimal toGrams() {
        return kilograms.multiply(BigDecimal.valueOf(1000));
    }
}
