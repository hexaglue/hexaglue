package com.example.ecommerce.domain.product;

import com.example.ecommerce.domain.shared.AggregateRoot;
import com.example.ecommerce.domain.order.Money;

import java.time.Instant;
import java.util.Objects;

/**
 * Product aggregate root representing a product in the catalog.
 *
 * AUDIT VIOLATION: ddd:aggregate-repository
 * This aggregate does not have a corresponding repository port defined.
 * The ProductRepository interface is missing from the ports.
 */
public class Product extends AggregateRoot<ProductId> {

    private final ProductId id;
    private String name;
    private String description;
    private String sku;
    private Money price;
    private ProductCategory category;
    private boolean active;
    private int stockQuantity;
    private final Instant createdAt;
    private Instant updatedAt;

    private Product(ProductId id, String name, String sku, Money price) {
        this.id = Objects.requireNonNull(id, "Product ID cannot be null");
        this.name = Objects.requireNonNull(name, "Product name cannot be null");
        this.sku = Objects.requireNonNull(sku, "SKU cannot be null");
        this.price = Objects.requireNonNull(price, "Price cannot be null");
        this.active = true;
        this.stockQuantity = 0;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public static Product create(String name, String sku, Money price) {
        Product product = new Product(ProductId.generate(), name, sku, price);
        product.registerEvent(new ProductAddEvent(product.id, name, price));
        return product;
    }

    public static Product reconstitute(ProductId id, String name, String sku, Money price,
                                       String description, boolean active, int stockQuantity) {
        Product product = new Product(id, name, sku, price);
        product.description = description;
        product.active = active;
        product.stockQuantity = stockQuantity;
        return product;
    }

    @Override
    public ProductId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getSku() {
        return sku;
    }

    public Money getPrice() {
        return price;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public boolean isActive() {
        return active;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateDetails(String name, String description) {
        this.name = Objects.requireNonNull(name, "Product name cannot be null");
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public void changePrice(Money newPrice) {
        Objects.requireNonNull(newPrice, "Price cannot be null");
        Money oldPrice = this.price;
        this.price = newPrice;
        this.updatedAt = Instant.now();
        registerEvent(new ProductPriceChangedEvent(id, oldPrice, newPrice));
    }

    public void assignCategory(ProductCategory category) {
        this.category = category;
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

    public void addStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.stockQuantity += quantity;
        this.updatedAt = Instant.now();
    }

    public void removeStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (quantity > stockQuantity) {
            throw new IllegalStateException("Insufficient stock");
        }
        this.stockQuantity -= quantity;
        this.updatedAt = Instant.now();
    }

    public boolean isInStock() {
        return stockQuantity > 0;
    }

    public boolean isAvailable() {
        return active && isInStock();
    }
}
