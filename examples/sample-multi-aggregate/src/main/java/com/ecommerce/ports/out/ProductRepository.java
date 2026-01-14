package com.ecommerce.ports.out;

import com.ecommerce.domain.product.Product;
import com.ecommerce.domain.product.ProductId;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for Product aggregate persistence.
 */
public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(ProductId id);

    List<Product> findByNameContaining(String name);

    List<Product> findAllActive();

    List<Product> findAll();

    void delete(Product product);
}
