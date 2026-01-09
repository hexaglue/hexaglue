package com.example.ecommerce.port.driven;

import com.example.ecommerce.domain.model.Order;
import com.example.ecommerce.domain.model.OrderId;
import com.example.ecommerce.domain.model.CustomerId;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for Order persistence.
 */
public interface OrderRepository {
    Order save(Order order);

    Optional<Order> findById(OrderId orderId);

    List<Order> findByCustomerId(CustomerId customerId);

    List<Order> findAll();

    void deleteById(OrderId orderId);

    boolean existsById(OrderId orderId);
}
