package com.example.ecommerce.port.driven;

import com.example.ecommerce.domain.model.ProductReview;
import com.example.ecommerce.domain.model.ProductReviewId;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for ProductReview persistence.
 */
public interface ProductReviewRepository {
    ProductReview save(ProductReview entity);

    Optional<ProductReview> findById(ProductReviewId id);

    List<ProductReview> findAll();

    void deleteById(ProductReviewId id);

    boolean existsById(ProductReviewId id);
}
