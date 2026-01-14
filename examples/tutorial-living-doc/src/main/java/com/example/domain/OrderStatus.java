package com.example.domain;

/**
 * Status of an order in its lifecycle.
 */
public enum OrderStatus {
    DRAFT,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
