package com.example.ecommerce.domain.model;

/**
 * Entity representing A refund transaction record.
 */
public class RefundTransaction {
    private final String id;
    private String name;

    public RefundTransaction(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
