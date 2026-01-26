package com.university.domain.course;

import java.util.UUID;

/**
 * Identifier for Lesson entity.
 */
public record LessonId(UUID value) {

    public static LessonId generate() {
        return new LessonId(UUID.randomUUID());
    }

    public static LessonId of(UUID value) {
        return new LessonId(value);
    }
}
