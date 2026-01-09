package com.example.ecommerce.domain.service;

import com.example.ecommerce.domain.model.Order;
import com.example.ecommerce.domain.model.Money;
import java.math.BigDecimal;

/**
 * Domain service for order pricing calculations.
 */
public class OrderPricingService {
    private static final BigDecimal TAX_RATE = new BigDecimal("0.20"); // 20% VAT
    private static final Money FREE_SHIPPING_THRESHOLD = Money.euro(new BigDecimal("50.00"));
    private static final Money SHIPPING_COST = Money.euro(new BigDecimal("5.00"));

    public Money calculateSubtotal(Order order) {
        return order.calculateTotal();
    }

    public Money calculateTax(Order order) {
        Money subtotal = calculateSubtotal(order);
        return new Money(
            subtotal.amount().multiply(TAX_RATE),
            subtotal.currency()
        );
    }

    public Money calculateShipping(Order order) {
        Money subtotal = calculateSubtotal(order);
        if (subtotal.isGreaterThan(FREE_SHIPPING_THRESHOLD)) {
            return Money.euro(BigDecimal.ZERO);
        }
        return SHIPPING_COST;
    }

    public Money calculateTotal(Order order) {
        Money subtotal = calculateSubtotal(order);
        Money tax = calculateTax(order);
        Money shipping = calculateShipping(order);
        return subtotal.add(tax).add(shipping);
    }
}
