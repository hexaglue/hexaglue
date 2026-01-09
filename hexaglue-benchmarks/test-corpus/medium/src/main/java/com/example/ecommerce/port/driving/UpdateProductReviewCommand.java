package com.example.ecommerce.port.driving;

/**
 * Command for updating ProductReview.
 */
public record UpdateProductReviewCommand(
    String name,
    String description
) {
}
