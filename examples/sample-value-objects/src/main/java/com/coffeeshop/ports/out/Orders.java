package com.coffeeshop.ports.out;

import com.coffeeshop.domain.order.Order;
import com.coffeeshop.domain.order.OrderId;
import java.util.List;
import java.util.Optional;

/**
 * Secondary port for order persistence.
 * Expected classification: REPOSITORY, DRIVEN (package "ports.out", name pattern)
 */
public interface Orders {

    /**
     * Saves an order.
     */
    Order save(Order order);

    /**
     * Finds an order by its ID.
     */
    Optional<Order> findById(OrderId id);

    /**
     * Lists all orders.
     */
    List<Order> findAll();

    /**
     * Deletes an order.
     */
    void delete(Order order);
}
