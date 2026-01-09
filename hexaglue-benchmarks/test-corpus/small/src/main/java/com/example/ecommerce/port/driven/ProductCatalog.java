package com.example.ecommerce.port.driven;

import com.example.ecommerce.domain.model.ProductId;
import com.example.ecommerce.domain.model.Money;
import java.util.Optional;

/**
 * Driven port (secondary) for accessing product information.
 */
public interface ProductCatalog {
    Optional<ProductInfo> findProduct(ProductId productId);

    boolean isProductAvailable(ProductId productId, int quantity);

    record ProductInfo(ProductId id, String name, Money price, int stockQuantity) {
    }
}
