package com.example.ports.out;

import com.example.domain.Product;
import com.example.domain.ProductId;
import java.util.Optional;

/**
 * Driven port (secondary) - products repository.
 * Package "ports.out" should classify this as DRIVEN.
 */
public interface Products {

    Product save(Product product);

    Optional<Product> findById(ProductId id);

    void delete(Product product);
}
