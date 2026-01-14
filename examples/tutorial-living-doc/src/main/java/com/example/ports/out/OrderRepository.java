package com.example.ports.out;

import com.example.domain.Order;
import com.example.domain.OrderId;
import java.util.Optional;

/**
 * Driven port for order persistence.
 * Defines the contract for storing and retrieving orders.
 */
public interface OrderRepository {

    /**
     * Saves an order.
     */
    Order save(Order order);

    /**
     * Finds an order by its ID.
     */
    Optional<Order> findById(OrderId id);

    /**
     * Deletes an order.
     */
    void delete(Order order);
}
