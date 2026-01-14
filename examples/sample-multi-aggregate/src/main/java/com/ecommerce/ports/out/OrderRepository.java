package com.ecommerce.ports.out;

import com.ecommerce.domain.customer.CustomerId;
import com.ecommerce.domain.order.Order;
import com.ecommerce.domain.order.OrderId;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for Order aggregate persistence.
 */
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(OrderId id);

    List<Order> findByCustomerId(CustomerId customerId);

    List<Order> findAll();

    void delete(Order order);
}
