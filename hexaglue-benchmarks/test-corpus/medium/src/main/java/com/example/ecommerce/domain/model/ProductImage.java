package com.example.ecommerce.domain.model;

/**
 * Entity representing a product image.
 */
public class ProductImage {
    private final String imageId;
    private final String url;
    private final String altText;
    private final int displayOrder;

    public ProductImage(String imageId, String url, String altText, int displayOrder) {
        if (imageId == null || imageId.isBlank()) {
            throw new IllegalArgumentException("Image ID cannot be null or blank");
        }
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL cannot be null or blank");
        }
        if (displayOrder < 0) {
            throw new IllegalArgumentException("Display order cannot be negative");
        }

        this.imageId = imageId;
        this.url = url;
        this.altText = altText;
        this.displayOrder = displayOrder;
    }

    public String getImageId() {
        return imageId;
    }

    public String getUrl() {
        return url;
    }

    public String getAltText() {
        return altText;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}
