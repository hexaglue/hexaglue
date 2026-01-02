package com.ecommerce.domain.order;

import com.ecommerce.domain.product.ProductId;

/**
 * Order line entity - part of Order aggregate.
 */
public class OrderLine {

    private final ProductId productId;
    private final String productName;
    private final Money unitPrice;
    private Quantity quantity;

    public OrderLine(ProductId productId, String productName, Money unitPrice, Quantity quantity) {
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public ProductId getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public Money lineTotal() {
        return unitPrice.multiply(quantity.value());
    }

    public void updateQuantity(Quantity newQuantity) {
        this.quantity = newQuantity;
    }
}
