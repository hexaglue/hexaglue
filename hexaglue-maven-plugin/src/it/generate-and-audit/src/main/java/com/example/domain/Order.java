package com.example.domain;

/**
 * An order aggregate root.
 */
public class Order {
    private final String id;
    private final String customerId;
    private final double totalAmount;

    public Order(String id, String customerId, double totalAmount) {
        this.id = id;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public double getTotalAmount() {
        return totalAmount;
    }
}
