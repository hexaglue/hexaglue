package com.ecommerce.ports.in;

import com.ecommerce.domain.order.Money;
import com.ecommerce.domain.product.Product;
import com.ecommerce.domain.product.ProductId;

import java.util.List;
import java.util.Optional;

/**
 * Driving port for product management use cases.
 */
public interface ManagingProducts {

    Product createProduct(String name, String description, Money price, int initialStock);

    Product updateProduct(ProductId productId, String name, String description, Money price);

    Product adjustStock(ProductId productId, int quantityDelta);

    Product activateProduct(ProductId productId);

    Product deactivateProduct(ProductId productId);

    Optional<Product> findProduct(ProductId productId);

    List<Product> searchProducts(String name);

    List<Product> listActiveProducts();

    List<Product> listAllProducts();

    void deleteProduct(ProductId productId);
}
