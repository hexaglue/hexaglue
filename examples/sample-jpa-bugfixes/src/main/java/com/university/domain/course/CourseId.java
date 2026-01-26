package com.university.domain.course;

import java.util.UUID;

/**
 * Identifier for Course aggregate.
 */
public record CourseId(UUID value) {

    public static CourseId generate() {
        return new CourseId(UUID.randomUUID());
    }

    public static CourseId of(UUID value) {
        return new CourseId(value);
    }
}
