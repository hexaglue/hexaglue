package com.example.ports.in;

import com.example.domain.Product;
import com.example.domain.ProductId;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Driving port (primary) - product catalog use case.
 * Package "ports.in" should classify this as DRIVING.
 */
public interface ProductCatalog {

    Product createProduct(String name, BigDecimal price);

    Optional<Product> findProduct(ProductId id);

    void updatePrice(ProductId id, BigDecimal newPrice);
}
