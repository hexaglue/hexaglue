package com.example.domain;

/** Book title value object. */
public record Title(String value) {

    public Title {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Title cannot be blank");
        }
    }
}
