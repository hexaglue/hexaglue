package com.example.validation.application;

import com.example.validation.domain.Order;
import com.example.validation.domain.OrderId;
import com.example.validation.port.OrderRepository;
import com.example.validation.port.OrderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Application service implementing the order use cases.
 *
 * <p>This is the CoreAppClass that orchestrates domain operations.
 * It implements a DRIVING port and depends on a DRIVEN port.
 *
 * <p>Classification: APPLICATION_SERVICE (inferred via semantic analysis)
 */
public class OrderApplicationService implements OrderService {

    private final OrderRepository repository;

    public OrderApplicationService(OrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public Order createOrder(String customerId) {
        Order order = new Order(OrderId.generate(), customerId);
        return repository.save(order);
    }

    @Override
    public Order addLineItem(OrderId orderId, String productId, int quantity, BigDecimal unitPrice) {
        Order order = repository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.addLine(productId, quantity, unitPrice);
        return repository.save(order);
    }

    @Override
    public Order confirmOrder(OrderId orderId) {
        Order order = repository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.confirm();
        return repository.save(order);
    }

    @Override
    public Optional<Order> getOrder(OrderId orderId) {
        return repository.findById(orderId);
    }

    @Override
    public List<Order> listOrdersForCustomer(String customerId) {
        return repository.findByCustomerId(customerId);
    }

    @Override
    public void cancelOrder(OrderId orderId) {
        repository.deleteById(orderId);
    }
}
