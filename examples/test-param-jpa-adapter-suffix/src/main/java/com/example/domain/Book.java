package com.example.domain;

import java.time.Instant;

/** Book aggregate root. */
public class Book {
    private final BookId id;
    private String title;
    private final Instant createdAt;

    public Book(BookId id, String title) {
        this.id = id;
        this.title = title;
        this.createdAt = Instant.now();
    }

    public BookId getId() { return id; }
    public String getTitle() { return title; }
    public Instant getCreatedAt() { return createdAt; }
    public void updateTitle(String title) { this.title = title; }
}
