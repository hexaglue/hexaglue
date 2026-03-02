package com.ecommerce.ports.in;

import com.ecommerce.domain.exception.BusinessRuleViolationException;
import com.ecommerce.domain.exception.ResourceNotFoundException;
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

    Product updateProduct(ProductId productId, String name, String description, Money price)
            throws ResourceNotFoundException;

    Product adjustStock(ProductId productId, int quantityDelta)
            throws ResourceNotFoundException, BusinessRuleViolationException;

    Product activateProduct(ProductId productId) throws ResourceNotFoundException;

    Product deactivateProduct(ProductId productId) throws ResourceNotFoundException;

    Optional<Product> findProduct(ProductId productId);

    List<Product> searchProducts(String name);

    List<Product> listActiveProducts();

    List<Product> listAllProducts();

    void deleteProduct(ProductId productId) throws ResourceNotFoundException;
}
