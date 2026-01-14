package com.coffeeshop.ports.in;

import com.coffeeshop.domain.order.LineItem;
import com.coffeeshop.domain.order.Location;
import com.coffeeshop.domain.order.Order;
import com.coffeeshop.domain.order.OrderId;
import java.util.Optional;

/**
 * Primary port for ordering coffee.
 * Expected classification: USE_CASE, DRIVING (package "ports.in")
 */
public interface OrderingCoffee {

    /**
     * Creates a new order for a customer.
     */
    Order createOrder(String customerName, Location location);

    /**
     * Adds an item to an existing order.
     */
    Order addItem(OrderId orderId, LineItem item);

    /**
     * Submits an order for preparation.
     */
    Order submitOrder(OrderId orderId);

    /**
     * Retrieves an order by its ID.
     */
    Optional<Order> findOrder(OrderId orderId);

    /**
     * Cancels a pending order.
     */
    void cancelOrder(OrderId orderId);
}
