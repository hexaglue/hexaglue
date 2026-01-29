package com.example.ecommerce.application.port.driven;

import com.example.ecommerce.domain.customer.CustomerId;
import com.example.ecommerce.domain.order.Order;
import com.example.ecommerce.domain.order.OrderId;

import java.util.List;
import java.util.Optional;

/**
 * Driven port defining the persistence contract for {@link com.example.ecommerce.domain.order.Order} aggregates.
 *
 * <p>This repository interface provides storage operations for orders including
 * save, find by identity, find by customer, existence check, deletion, and counting.
 * The customer-based query supports the order history feature in the customer dashboard.
 */
public interface OrderRepository {

    /**
     * Saves an order.
     */
    void save(Order order);

    /**
     * Finds an order by its ID.
     */
    Optional<Order> findById(OrderId id);

    /**
     * Finds all orders for a customer.
     */
    List<Order> findByCustomerId(CustomerId customerId);

    /**
     * Deletes an order.
     */
    void delete(OrderId id);

    /**
     * Checks if an order exists.
     */
    boolean exists(OrderId id);

    /**
     * Counts all orders.
     */
    long count();
}
