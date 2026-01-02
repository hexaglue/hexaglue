package com.example.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Orders repository interface.
 * Should be classified as REPOSITORY (DRIVEN port).
 */
public interface Orders {

    Order save(Order order);

    Optional<Order> findById(UUID id);

    void delete(Order order);
}
