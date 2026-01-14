package com.coffeeshop.domain.order;

import java.math.BigDecimal;

/**
 * A line item in an order.
 * Expected classification: VALUE_OBJECT (record, immutable, no identity)
 */
public record LineItem(
        String productName,
        int quantity,
        BigDecimal unitPrice
) {
    public LineItem {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
    }

    public BigDecimal totalPrice() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
