package com.example.ecommerce.domain.exception;

import com.example.ecommerce.domain.model.OrderId;

/**
 * Exception thrown when an order is not found.
 */
public class OrderNotFoundException extends DomainException {
    private final OrderId orderId;

    public OrderNotFoundException(OrderId orderId) {
        super("Order not found: " + orderId);
        this.orderId = orderId;
    }

    public OrderId getOrderId() {
        return orderId;
    }
}
