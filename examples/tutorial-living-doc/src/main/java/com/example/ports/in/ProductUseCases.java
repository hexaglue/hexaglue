package com.example.ports.in;

import com.example.domain.Money;
import com.example.domain.Product;
import com.example.domain.ProductId;
import java.util.List;
import java.util.Optional;

/**
 * Driving port for product operations.
 * Defines the use cases available to external actors.
 */
public interface ProductUseCases {

    /**
     * Creates a new product in the catalog.
     */
    Product createProduct(String name, String description, Money price);

    /**
     * Updates the price of an existing product.
     */
    void updatePrice(ProductId productId, Money newPrice);

    /**
     * Retrieves a product by its ID.
     */
    Optional<Product> getProduct(ProductId productId);

    /**
     * Lists all products in the catalog.
     */
    List<Product> listProducts();
}
