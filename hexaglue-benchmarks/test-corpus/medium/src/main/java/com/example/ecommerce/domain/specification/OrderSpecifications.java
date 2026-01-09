package com.example.ecommerce.domain.specification;

import com.example.ecommerce.domain.model.Order;
import com.example.ecommerce.domain.model.OrderStatus;
import com.example.ecommerce.domain.model.Money;

/**
 * Specifications for Order queries and business rules.
 */
public class OrderSpecifications {

    public static Specification<Order> isPending() {
        return order -> order.getStatus() == OrderStatus.PENDING;
    }

    public static Specification<Order> isConfirmed() {
        return order -> order.getStatus() == OrderStatus.CONFIRMED;
    }

    public static Specification<Order> isShipped() {
        return order -> order.getStatus() == OrderStatus.SHIPPED;
    }

    public static Specification<Order> isDelivered() {
        return order -> order.getStatus() == OrderStatus.DELIVERED;
    }

    public static Specification<Order> isCancelled() {
        return order -> order.getStatus() == OrderStatus.CANCELLED;
    }

    public static Specification<Order> totalGreaterThan(Money amount) {
        return order -> order.calculateTotal().isGreaterThan(amount);
    }

    public static Specification<Order> canBeCancelled() {
        return order -> order.getStatus() != OrderStatus.DELIVERED;
    }

    public static Specification<Order> canBeShipped() {
        return order -> order.getStatus() == OrderStatus.CONFIRMED;
    }
}
