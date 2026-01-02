package com.example.domain;

import java.util.UUID;

/**
 * Order aggregate root.
 * Should be classified as AGGREGATE_ROOT because it's used in Orders repository.
 */
public class Order {

    private final UUID id;
    private final String customerName;
    private int quantity;

    public Order(UUID id, String customerName, int quantity) {
        this.id = id;
        this.customerName = customerName;
        this.quantity = quantity;
    }

    public UUID getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void updateQuantity(int newQuantity) {
        this.quantity = newQuantity;
    }
}
