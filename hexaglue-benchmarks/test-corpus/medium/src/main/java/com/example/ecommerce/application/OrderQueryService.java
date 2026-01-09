package com.example.ecommerce.application;

import com.example.ecommerce.domain.model.Order;
import com.example.ecommerce.domain.model.OrderId;
import com.example.ecommerce.domain.model.CustomerId;
import com.example.ecommerce.port.driven.OrderRepository;
import java.util.List;
import java.util.Optional;

/**
 * Query service for order read operations.
 */
public class OrderQueryService {
    private final OrderRepository orderRepository;

    public OrderQueryService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Optional<Order> findById(OrderId orderId) {
        return orderRepository.findById(orderId);
    }

    public List<Order> findByCustomerId(CustomerId customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    public List<Order> findAll() {
        return orderRepository.findAll();
    }
}
