package io.hexaglue.arch.integration.fixtures.domain;

import java.math.BigDecimal;

/**
 * An order line item (internal to Order aggregate).
 *
 * <p>This is a simple class without explicit annotation - it should be
 * classified as UNCLASSIFIED by the standard classifiers.</p>
 */
public class OrderLine {

    private final String productId;
    private final int quantity;
    private final Money unitPrice;

    public OrderLine(String productId, int quantity, Money unitPrice) {
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public Money subtotal() {
        return new Money(
                unitPrice.amount().multiply(BigDecimal.valueOf(quantity)), unitPrice.currency());
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }
}
