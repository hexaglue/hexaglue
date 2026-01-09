package com.example.ecommerce.port.driving;

/**
 * Command for creating ProductReview.
 */
public record CreateProductReviewCommand(
    String name,
    String description
) {
}
