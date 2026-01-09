package com.example.domain;

import java.util.Optional;

/**
 * Repository interface for products.
 */
public interface Products {
    Optional<Product> findById(String id);
    void save(Product product);
}
