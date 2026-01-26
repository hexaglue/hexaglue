package com.university.domain.student;

import java.util.UUID;

/**
 * Identifier for Student aggregate.
 *
 * <p>Demonstrates proper identifier pattern for DDD aggregates.
 */
public record StudentId(UUID value) {

    public static StudentId generate() {
        return new StudentId(UUID.randomUUID());
    }

    public static StudentId of(UUID value) {
        return new StudentId(value);
    }
}
