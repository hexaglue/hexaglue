package com.example.domain;

/**
 * An order line within an Order aggregate.
 * Represents a product and its quantity in an order.
 */
public class OrderLine {
    private final ProductId productId;
    private final String productName;
    private final Quantity quantity;
    private final Money unitPrice;

    public OrderLine(ProductId productId, String productName, Quantity quantity, Money unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public ProductId getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public Money getLineTotal() {
        return unitPrice.multiply(quantity.value());
    }
}
