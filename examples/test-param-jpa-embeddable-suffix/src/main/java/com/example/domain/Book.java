package com.example.domain;

import java.time.Instant;

/** Book aggregate root. */
public class Book {
    private final BookId id;
    private final String title;
    private Address shippingAddress;
    private final Instant createdAt;

    public Book(BookId id, String title, Address shippingAddress) {
        this.id = id;
        this.title = title;
        this.shippingAddress = shippingAddress;
        this.createdAt = Instant.now();
    }

    public BookId getId() { return id; }
    public String getTitle() { return title; }
    public Address getShippingAddress() { return shippingAddress; }
    public Instant getCreatedAt() { return createdAt; }
    public void updateAddress(Address address) { this.shippingAddress = address; }
}
