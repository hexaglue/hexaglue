package com.regression.domain.order;

import com.regression.domain.shared.Money;
import com.regression.domain.shared.Quantity;

/**
 * Entity representing a line item in an order.
 * <p>
 * Tests M13: Quantity is a simple wrapper and should be unwrapped to int.
 * Tests M11: unitPrice (Money) is a complex value object needing embeddable.
 */
public record OrderLine(
        ProductId productId,
        String productName,
        Money unitPrice,
        Quantity quantity) {

    public OrderLine {
        if (productId == null) {
            throw new IllegalArgumentException("productId cannot be null");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("productName cannot be null or blank");
        }
        if (unitPrice == null) {
            throw new IllegalArgumentException("unitPrice cannot be null");
        }
        if (quantity == null) {
            throw new IllegalArgumentException("quantity cannot be null");
        }
    }

    public static OrderLine of(ProductId productId, String productName, Money unitPrice, Quantity quantity) {
        return new OrderLine(productId, productName, unitPrice, quantity);
    }

    public Money totalPrice() {
        return Money.of(unitPrice.amount().multiply(java.math.BigDecimal.valueOf(quantity.value())), unitPrice.currency());
    }
}
