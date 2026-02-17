package com.example.ports.out;

import com.example.domain.order.Order;
import com.example.domain.order.OrderId;
import java.util.Optional;

/** Secondary port for order persistence. */
public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(OrderId id);
}
