package com.ecommerce.application;

import com.ecommerce.domain.customer.CustomerId;
import com.ecommerce.domain.order.Address;
import com.ecommerce.domain.order.Order;
import com.ecommerce.domain.order.OrderId;
import com.ecommerce.domain.order.OrderLine;
import com.ecommerce.domain.order.Quantity;
import com.ecommerce.domain.product.Product;
import com.ecommerce.domain.product.ProductId;
import com.ecommerce.ports.in.OrderingProducts;
import com.ecommerce.ports.out.OrderRepository;
import com.ecommerce.ports.out.ProductRepository;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Application service implementing ordering use cases.
 */
public class OrderService implements OrderingProducts {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Override
    public Order createOrder(CustomerId customerId, Address shippingAddress) {
        Order order = new Order(OrderId.generate(), customerId);
        order.setShippingAddress(shippingAddress);
        return orderRepository.save(order);
    }

    @Override
    public Order addLineItem(OrderId orderId, ProductId productId, int quantity) {
        Order order = findOrderOrThrow(orderId);
        Product product = findProductOrThrow(productId);
        OrderLine line = new OrderLine(productId, product.getName(), product.getPrice(), Quantity.of(quantity));
        order.addLine(line);
        return orderRepository.save(order);
    }

    @Override
    public Order removeLineItem(OrderId orderId, ProductId productId) {
        Order order = findOrderOrThrow(orderId);
        order.removeLine(productId);
        return orderRepository.save(order);
    }

    @Override
    public Order confirmOrder(OrderId orderId) {
        Order order = findOrderOrThrow(orderId);
        order.place();
        return orderRepository.save(order);
    }

    @Override
    public Order cancelOrder(OrderId orderId) {
        Order order = findOrderOrThrow(orderId);
        order.cancel();
        return orderRepository.save(order);
    }

    @Override
    public Order shipOrder(OrderId orderId) {
        Order order = findOrderOrThrow(orderId);
        order.ship();
        return orderRepository.save(order);
    }

    @Override
    public Order completeOrder(OrderId orderId) {
        Order order = findOrderOrThrow(orderId);
        order.deliver();
        return orderRepository.save(order);
    }

    @Override
    public Order getOrder(OrderId orderId) {
        return findOrderOrThrow(orderId);
    }

    @Override
    public List<Order> getCustomerOrders(CustomerId customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    private Order findOrderOrThrow(OrderId orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
    }

    private Product findProductOrThrow(ProductId productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));
    }
}
