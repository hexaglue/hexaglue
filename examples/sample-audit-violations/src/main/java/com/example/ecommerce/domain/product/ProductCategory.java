package com.example.ecommerce.domain.product;

import com.example.ecommerce.domain.shared.Entity;

import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a product category.
 */
public class ProductCategory extends Entity<UUID> {

    private final UUID id;
    private String name;
    private String description;
    private ProductCategory parent;

    public ProductCategory(String name, String description) {
        this.id = UUID.randomUUID();
        this.name = Objects.requireNonNull(name, "Category name cannot be null");
        this.description = description;
    }

    public ProductCategory(String name, String description, ProductCategory parent) {
        this(name, description);
        this.parent = parent;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "Category name cannot be null");
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProductCategory getParent() {
        return parent;
    }

    public void setParent(ProductCategory parent) {
        this.parent = parent;
    }

    public boolean hasParent() {
        return parent != null;
    }

    public String getFullPath() {
        if (parent == null) {
            return name;
        }
        return parent.getFullPath() + " > " + name;
    }
}
