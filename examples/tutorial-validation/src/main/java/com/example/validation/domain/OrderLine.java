package com.example.validation.domain;

import java.math.BigDecimal;
import org.jmolecules.ddd.annotation.Entity;

/**
 * Order line entity within the Order aggregate.
 *
 * <p>Classification: EXPLICIT via @Entity annotation.
 *
 * <p>This entity is part of the Order aggregate and represents a single line item.
 * It demonstrates an entity that belongs to an aggregate root.
 */
@Entity
public class OrderLine {

    private final String productId;
    private final int quantity;
    private final BigDecimal unitPrice;

    public OrderLine(String productId, int quantity, BigDecimal unitPrice) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("Product ID cannot be null or blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price cannot be null or negative");
        }

        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
