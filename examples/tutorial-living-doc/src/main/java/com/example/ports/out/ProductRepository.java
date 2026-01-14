package com.example.ports.out;

import com.example.domain.Product;
import com.example.domain.ProductId;
import java.util.List;
import java.util.Optional;

/**
 * Driven port for product persistence.
 * Defines the contract for storing and retrieving products.
 */
public interface ProductRepository {

    /**
     * Saves a product.
     */
    Product save(Product product);

    /**
     * Finds a product by its ID.
     */
    Optional<Product> findById(ProductId id);

    /**
     * Finds all products.
     */
    List<Product> findAll();

    /**
     * Deletes a product.
     */
    void delete(Product product);
}
