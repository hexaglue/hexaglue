package com.university.domain.course;

import java.util.UUID;

/**
 * Identifier for Tag aggregate.
 */
public record TagId(UUID value) {

    public static TagId generate() {
        return new TagId(UUID.randomUUID());
    }

    public static TagId of(UUID value) {
        return new TagId(value);
    }
}
