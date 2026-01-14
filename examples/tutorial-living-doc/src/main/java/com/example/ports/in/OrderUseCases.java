package com.example.ports.in;

import com.example.domain.Order;
import com.example.domain.OrderId;
import com.example.domain.ProductId;
import java.util.Optional;

/**
 * Driving port for order operations.
 * Defines the use cases available to external actors.
 */
public interface OrderUseCases {

    /**
     * Creates a new order for a customer.
     */
    Order createOrder(String customerName);

    /**
     * Adds a product to an existing order.
     */
    void addProduct(OrderId orderId, ProductId productId, int quantity);

    /**
     * Confirms an order, making it ready for processing.
     */
    void confirmOrder(OrderId orderId);

    /**
     * Retrieves an order by its ID.
     */
    Optional<Order> getOrder(OrderId orderId);
}
