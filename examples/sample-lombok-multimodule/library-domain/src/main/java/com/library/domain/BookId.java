package com.library.domain;

import java.util.UUID;

/**
 * Book identifier.
 *
 * @since 6.1.0
 */
public record BookId(UUID value) {

    public static BookId generate() {
        return new BookId(UUID.randomUUID());
    }
}
