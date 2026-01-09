package com.example.ecommerce.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root representing a Product in the catalog.
 */
public class Product {
    private final ProductId id;
    private String name;
    private String description;
    private SKU sku;
    private Money price;
    private Category category;
    private Weight weight;
    private Dimensions dimensions;
    private boolean active;
    private final List<ProductImage> images;
    private final Instant createdAt;
    private Instant updatedAt;

    public Product(
        ProductId id,
        String name,
        String description,
        SKU sku,
        Money price,
        Category category,
        Weight weight,
        Dimensions dimensions
    ) {
        if (id == null) {
            throw new IllegalArgumentException("ProductId cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be null or blank");
        }
        if (sku == null) {
            throw new IllegalArgumentException("SKU cannot be null");
        }
        if (price == null) {
            throw new IllegalArgumentException("Price cannot be null");
        }

        this.id = id;
        this.name = name;
        this.description = description;
        this.sku = sku;
        this.price = price;
        this.category = category;
        this.weight = weight;
        this.dimensions = dimensions;
        this.active = true;
        this.images = new ArrayList<>();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void updatePrice(Money newPrice) {
        if (newPrice == null) {
            throw new IllegalArgumentException("Price cannot be null");
        }
        this.price = newPrice;
        this.updatedAt = Instant.now();
    }

    public void updateDescription(String newDescription) {
        this.description = newDescription;
        this.updatedAt = Instant.now();
    }

    public void addImage(ProductImage image) {
        if (image == null) {
            throw new IllegalArgumentException("Image cannot be null");
        }
        this.images.add(image);
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.active = true;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    public ProductId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public SKU getSku() {
        return sku;
    }

    public Money getPrice() {
        return price;
    }

    public Category getCategory() {
        return category;
    }

    public Weight getWeight() {
        return weight;
    }

    public Dimensions getDimensions() {
        return dimensions;
    }

    public boolean isActive() {
        return active;
    }

    public List<ProductImage> getImages() {
        return Collections.unmodifiableList(images);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
