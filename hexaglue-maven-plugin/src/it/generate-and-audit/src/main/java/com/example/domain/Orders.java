package com.example.domain;

import java.util.Optional;

/**
 * Repository interface for orders.
 */
public interface Orders {
    Optional<Order> findById(String id);
    void save(Order order);
}
