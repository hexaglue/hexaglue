package com.example.ecommerce.domain.model;

/**
 * Entity representing A payment transaction record.
 */
public class PaymentTransaction {
    private final String id;
    private String name;

    public PaymentTransaction(String id, String name) {
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
