package com.example.ecommerce.application.service;

import com.example.ecommerce.application.port.driving.OrderUseCase;
import com.example.ecommerce.application.port.driven.OrderRepository;
import com.example.ecommerce.application.port.driven.PaymentGateway;
import com.example.ecommerce.domain.customer.CustomerId;
import com.example.ecommerce.domain.order.Order;
import com.example.ecommerce.domain.order.OrderId;
import com.example.ecommerce.domain.order.OrderLine;
import com.example.ecommerce.domain.order.Money;
import com.example.ecommerce.domain.product.ProductId;
import com.example.ecommerce.infrastructure.persistence.JpaOrderRepository;

import java.util.List;

/**
 * Application service orchestrating the complete order lifecycle.
 *
 * <p>This service implements the {@link OrderUseCase} driving port and coordinates
 * order creation, line item management, placement, payment confirmation via the
 * payment gateway, shipping, and cancellation. It retrieves and persists orders
 * through the repository port.
 *
 * <p>AUDIT VIOLATION: hex:dependency-inversion.
 * This application service depends directly on the concrete JpaOrderRepository
 * instead of depending only on the OrderRepository port interface, breaking
 * the dependency inversion principle of hexagonal architecture.
 */
public class OrderApplicationService implements OrderUseCase {

    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;

    // VIOLATION: Direct dependency on concrete infrastructure class
    private final JpaOrderRepository jpaRepository;

    public OrderApplicationService(OrderRepository orderRepository,
                                   PaymentGateway paymentGateway,
                                   JpaOrderRepository jpaRepository) {
        this.orderRepository = orderRepository;
        this.paymentGateway = paymentGateway;
        this.jpaRepository = jpaRepository; // VIOLATION!
    }

    @Override
    public Order createOrder(CustomerId customerId) {
        Order order = Order.create(customerId);
        orderRepository.save(order);
        return order;
    }

    @Override
    public void addLineToOrder(OrderId orderId, ProductId productId, String productName,
                               int quantity, Money unitPrice) {
        Order order = getOrder(orderId);
        OrderLine line = new OrderLine(productId, productName, quantity, unitPrice);
        order.addLine(line);
        orderRepository.save(order);
    }

    @Override
    public void placeOrder(OrderId orderId) {
        Order order = getOrder(orderId);
        order.place();
        orderRepository.save(order);
    }

    @Override
    public void confirmPayment(OrderId orderId) {
        Order order = getOrder(orderId);
        PaymentGateway.PaymentResult result = paymentGateway.processPayment(
                orderId, order.getTotalAmount(), "payment_token");

        if (result.successful()) {
            order.confirmPayment();
            orderRepository.save(order);
        } else {
            throw new RuntimeException("Payment failed: " + result.errorMessage());
        }
    }

    @Override
    public void shipOrder(OrderId orderId, String trackingNumber, String carrier) {
        Order order = getOrder(orderId);
        order.startProcessing();
        order.ship(trackingNumber, carrier);
        orderRepository.save(order);
    }

    @Override
    public void cancelOrder(OrderId orderId, String reason) {
        Order order = getOrder(orderId);
        order.cancel(reason);
        orderRepository.save(order);
    }

    @Override
    public Order getOrder(OrderId orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    @Override
    public List<Order> getOrdersForCustomer(CustomerId customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    /**
     * VIOLATION: Method using concrete repository directly
     * This bypasses the port interface.
     */
    public void forceFlush() {
        jpaRepository.flush();
    }
}
