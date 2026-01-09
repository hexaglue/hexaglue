package com.example.ecommerce.domain.model;

/**
 * Entity representing a line item in an order.
 */
public class OrderLine {
    private final ProductId productId;
    private final String productName;
    private final int quantity;
    private final Money unitPrice;

    public OrderLine(ProductId productId, String productName, int quantity, Money unitPrice) {
        if (productId == null) {
            throw new IllegalArgumentException("ProductId cannot be null");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be null or blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (unitPrice == null) {
            throw new IllegalArgumentException("Unit price cannot be null");
        }
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public Money calculateTotal() {
        return unitPrice.multiply(quantity);
    }

    public ProductId getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }
}
