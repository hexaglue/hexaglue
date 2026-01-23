package com.regression.application;

import com.regression.domain.customer.CustomerId;
import com.regression.domain.order.Order;
import com.regression.domain.order.OrderId;
import com.regression.domain.order.OrderLine;
import com.regression.domain.shared.Money;
import com.regression.ports.in.OrderUseCase;
import com.regression.ports.out.CustomerRepository;
import com.regression.ports.out.OrderRepository;

import java.util.List;
import java.util.Optional;

/**
 * Application service implementing order use cases.
 */
public class OrderService implements OrderUseCase {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;

    public OrderService(OrderRepository orderRepository, CustomerRepository customerRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    public Order createOrder(CustomerId customerId, String currency) {
        customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        var order = Order.create(customerId, currency);
        return orderRepository.save(order);
    }

    @Override
    public Order addLineToOrder(OrderId orderId, OrderLine line) {
        return orderRepository.findById(orderId)
                .map(order -> order.addLine(line))
                .map(orderRepository::save)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    @Override
    public Order applyDiscount(OrderId orderId, Money discount) {
        return orderRepository.findById(orderId)
                .map(order -> order.applyDiscount(discount))
                .map(orderRepository::save)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    @Override
    public Order confirmOrder(OrderId orderId) {
        return orderRepository.findById(orderId)
                .map(Order::confirm)
                .map(orderRepository::save)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    @Override
    public Order shipOrder(OrderId orderId) {
        return orderRepository.findById(orderId)
                .map(Order::ship)
                .map(orderRepository::save)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    @Override
    public Optional<Order> findOrder(OrderId id) {
        return orderRepository.findById(id);
    }

    @Override
    public List<Order> findOrdersByCustomer(CustomerId customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    @Override
    public List<Order> findUrgentOrders() {
        return orderRepository.findByUrgent(true);
    }
}
