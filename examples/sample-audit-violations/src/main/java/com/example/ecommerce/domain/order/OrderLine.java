package com.example.ecommerce.domain.order;

import com.example.ecommerce.domain.shared.Entity;
import com.example.ecommerce.domain.product.ProductId;

/**
 * Entity representing a single line item within an {@link Order}.
 *
 * <p>Each OrderLine captures a product reference, its display name, the quantity
 * ordered, and the unit price at the time of ordering. The total price is computed
 * by multiplying the unit price by the quantity.
 *
 * <p>OrderLine is an internal entity of the Order aggregate and should only be
 * accessed and modified through the aggregate root.
 *
 * <p>AUDIT VIOLATION: ddd:entity-identity.
 * This entity does not have a proper identity field.
 * The getId() method returns null, which violates the identity requirement.
 */
public class OrderLine extends Entity<Long> {

    // VIOLATION: No identity field defined!
    private final ProductId productId;
    private final String productName;
    private final int quantity;
    private final Money unitPrice;

    public OrderLine(ProductId productId, String productName, int quantity, Money unitPrice) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    /**
     * VIOLATION: Returns null - entity has no identity!
     */
    @Override
    public Long getId() {
        return null; // Missing identity
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

    public Money getTotalPrice() {
        return unitPrice.multiply(quantity);
    }

    public OrderLine withQuantity(int newQuantity) {
        return new OrderLine(productId, productName, newQuantity, unitPrice);
    }
}
