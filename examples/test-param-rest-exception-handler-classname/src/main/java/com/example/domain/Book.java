package com.example.domain;

import java.time.Instant;

/** Book aggregate root. */
public class Book {

    private final BookId id;
    private String title;
    private String author;
    private final Instant createdAt;

    public Book(BookId id, String title, String author) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.createdAt = Instant.now();
    }

    public BookId getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateAuthor(String author) {
        this.author = author;
    }
}
