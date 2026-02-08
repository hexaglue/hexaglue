package com.example.ports.out;

import com.example.domain.Product;
import com.example.domain.ProductId;
import java.util.Optional;

/** Secondary port for product persistence. */
public interface ProductRepository {
    Product save(Product product);

    Optional<Product> findById(ProductId id);
}
