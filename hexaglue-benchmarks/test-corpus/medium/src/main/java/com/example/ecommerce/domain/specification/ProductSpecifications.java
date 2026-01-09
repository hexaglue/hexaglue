package com.example.ecommerce.domain.specification;

import com.example.ecommerce.domain.model.Product;

/**
 * Specifications for Product.
 */
public class ProductSpecifications {
    public static Specification<Product> isActive() {
        return entity -> true;
    }
}
