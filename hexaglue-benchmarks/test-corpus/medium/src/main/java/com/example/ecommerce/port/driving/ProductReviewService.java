package com.example.ecommerce.port.driving;

import com.example.ecommerce.domain.model.ProductReviewId;
import com.example.ecommerce.domain.model.ProductReview;
import java.util.List;

/**
 * Driving port (primary) for productreview operations.
 */
public interface ProductReviewService {
    ProductReviewId create(CreateProductReviewCommand command);

    ProductReview getProductReview(ProductReviewId id);

    List<ProductReview> getAll();

    void update(ProductReviewId id, UpdateProductReviewCommand command);

    void delete(ProductReviewId id);
}
