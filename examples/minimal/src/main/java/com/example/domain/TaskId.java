package com.example.domain;

import java.util.UUID;

/**
 * Task identifier.
 * This is a value object wrapping the task's unique identifier.
 */
public record TaskId(UUID value) {

    /**
     * Creates a new random TaskId.
     */
    public static TaskId generate() {
        return new TaskId(UUID.randomUUID());
    }

    /**
     * Creates a TaskId from a string representation.
     */
    public static TaskId fromString(String id) {
        return new TaskId(UUID.fromString(id));
    }
}
