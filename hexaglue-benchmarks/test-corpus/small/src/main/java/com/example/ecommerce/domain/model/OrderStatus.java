package com.example.ecommerce.domain.model;

/**
 * Value Object representing the status of an order.
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
