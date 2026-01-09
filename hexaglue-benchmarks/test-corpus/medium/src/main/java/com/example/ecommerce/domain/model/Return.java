package com.example.ecommerce.domain.model;

import java.time.Instant;

/**
 * Aggregate Root representing a Return.
 */
public class Return {
    private final ReturnId id;
    private String name;
    private final Instant createdAt;
    private Instant updatedAt;

    public Return(ReturnId id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("ReturnId cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.id = id;
        this.name = name;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public ReturnId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
