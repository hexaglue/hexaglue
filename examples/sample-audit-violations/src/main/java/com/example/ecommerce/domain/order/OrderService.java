package com.example.ecommerce.domain.order;

import com.example.ecommerce.domain.customer.CustomerId;
import com.example.ecommerce.domain.product.ProductId;

import java.math.BigDecimal;
import java.util.List;

/**
 * Domain service encapsulating order-related business logic that spans multiple aggregates.
 *
 * <p>This service handles cross-cutting order operations that do not naturally belong
 * to the {@link Order} aggregate root, including shipping cost calculation based on
 * destination country, product availability validation, loyalty discount application,
 * and multi-line order creation.
 *
 * <p>Shipping rules: base rate of 5.99 EUR for France, 12.99 EUR for EU countries,
 * 24.99 EUR for international, and a flat 9.99 EUR for bulk orders (more than 5 lines).
 * Loyalty discounts range from 5% (tier 1) to 15% (tier 3).
 */
public class OrderService {

    /**
     * Calculates shipping cost based on order details.
     */
    public Money calculateShippingCost(Order order, String destinationCountry) {
        Money baseShipping = Money.euros(new BigDecimal("5.99"));
        int lineCount = order.getLineCount();

        if (lineCount > 5) {
            // Bulk shipping discount
            return Money.euros(new BigDecimal("9.99"));
        }

        if ("FR".equals(destinationCountry)) {
            return baseShipping;
        } else if (isEuropeanUnion(destinationCountry)) {
            return Money.euros(new BigDecimal("12.99"));
        } else {
            return Money.euros(new BigDecimal("24.99"));
        }
    }

    /**
     * Validates that all products in the order are still available.
     */
    public boolean validateOrderAvailability(Order order, List<ProductId> availableProducts) {
        return order.getLines().stream()
                .map(OrderLine::getProductId)
                .allMatch(availableProducts::contains);
    }

    /**
     * Applies discount based on customer loyalty tier.
     */
    public Money applyLoyaltyDiscount(Money amount, int loyaltyTier) {
        BigDecimal discountPercentage = switch (loyaltyTier) {
            case 1 -> new BigDecimal("0.05");  // 5% for tier 1
            case 2 -> new BigDecimal("0.10");  // 10% for tier 2
            case 3 -> new BigDecimal("0.15");  // 15% for tier 3
            default -> BigDecimal.ZERO;
        };

        BigDecimal discountedAmount = amount.getAmount()
                .multiply(BigDecimal.ONE.subtract(discountPercentage));
        return new Money(discountedAmount, amount.getCurrency());
    }

    /**
     * Creates an order from a list of items.
     */
    public Order createOrderWithLines(CustomerId customerId, List<OrderLineRequest> requests) {
        Order order = Order.create(customerId);
        for (OrderLineRequest request : requests) {
            OrderLine line = new OrderLine(
                    request.productId(),
                    request.productName(),
                    request.quantity(),
                    request.unitPrice()
            );
            order.addLine(line);
        }
        return order;
    }

    private boolean isEuropeanUnion(String countryCode) {
        return List.of("DE", "IT", "ES", "PT", "BE", "NL", "LU", "AT", "IE", "GR",
                "FI", "SE", "DK", "PL", "CZ", "SK", "HU", "RO", "BG", "HR",
                "SI", "EE", "LV", "LT", "MT", "CY").contains(countryCode);
    }

    /**
     * Request object for creating order lines.
     */
    public record OrderLineRequest(
            ProductId productId,
            String productName,
            int quantity,
            Money unitPrice
    ) {}
}
