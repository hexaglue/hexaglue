package com.example.ecommerce.application;

import com.example.ecommerce.domain.model.*;
import com.example.ecommerce.port.driven.CustomerRepository;
import com.example.ecommerce.port.driven.OrderRepository;
import com.example.ecommerce.port.driven.ProductCatalog;
import com.example.ecommerce.port.driven.EventPublisher;
import com.example.ecommerce.port.driving.OrderService;
import com.example.ecommerce.port.driving.PlaceOrderCommand;
import java.util.ArrayList;
import java.util.List;

/**
 * Use case implementation for placing an order.
 */
public class PlaceOrderUseCase implements OrderService {
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductCatalog productCatalog;
    private final EventPublisher eventPublisher;

    public PlaceOrderUseCase(
        OrderRepository orderRepository,
        CustomerRepository customerRepository,
        ProductCatalog productCatalog,
        EventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productCatalog = productCatalog;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public OrderId placeOrder(PlaceOrderCommand command) {
        // Validate customer exists
        Customer customer = customerRepository.findById(command.customerId())
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + command.customerId()));

        // Build order lines
        List<OrderLine> orderLines = new ArrayList<>();
        for (PlaceOrderCommand.OrderLineItem item : command.items()) {
            ProductCatalog.ProductInfo product = productCatalog.findProduct(item.productId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + item.productId()));

            if (!productCatalog.isProductAvailable(item.productId(), item.quantity())) {
                throw new IllegalStateException("Product not available: " + product.name());
            }

            OrderLine line = new OrderLine(
                product.id(),
                product.name(),
                item.quantity(),
                product.price()
            );
            orderLines.add(line);
        }

        // Create and save order
        Order order = new Order(
            OrderId.generate(),
            command.customerId(),
            command.shippingAddress(),
            orderLines
        );

        Order savedOrder = orderRepository.save(order);

        // Publish event
        eventPublisher.publish(new OrderPlacedEvent(savedOrder.getId(), savedOrder.getCustomerId()));

        return savedOrder.getId();
    }

    @Override
    public Order getOrder(OrderId orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    @Override
    public List<Order> getCustomerOrders(CustomerId customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    @Override
    public void confirmOrder(OrderId orderId) {
        Order order = getOrder(orderId);
        order.confirm();
        orderRepository.save(order);
        eventPublisher.publish(new OrderConfirmedEvent(orderId));
    }

    @Override
    public void shipOrder(OrderId orderId) {
        Order order = getOrder(orderId);
        order.ship();
        orderRepository.save(order);
        eventPublisher.publish(new OrderShippedEvent(orderId));
    }

    @Override
    public void cancelOrder(OrderId orderId) {
        Order order = getOrder(orderId);
        order.cancel();
        orderRepository.save(order);
        eventPublisher.publish(new OrderCancelledEvent(orderId));
    }

    private record OrderPlacedEvent(OrderId orderId, CustomerId customerId) implements EventPublisher.DomainEvent {
        @Override
        public String eventType() {
            return "OrderPlaced";
        }
    }

    private record OrderConfirmedEvent(OrderId orderId) implements EventPublisher.DomainEvent {
        @Override
        public String eventType() {
            return "OrderConfirmed";
        }
    }

    private record OrderShippedEvent(OrderId orderId) implements EventPublisher.DomainEvent {
        @Override
        public String eventType() {
            return "OrderShipped";
        }
    }

    private record OrderCancelledEvent(OrderId orderId) implements EventPublisher.DomainEvent {
        @Override
        public String eventType() {
            return "OrderCancelled";
        }
    }
}
