package com.example.domain;

import java.util.UUID;

/** Book identifier. */
public record BookId(UUID value) {
    public static BookId generate() { return new BookId(UUID.randomUUID()); }
}
