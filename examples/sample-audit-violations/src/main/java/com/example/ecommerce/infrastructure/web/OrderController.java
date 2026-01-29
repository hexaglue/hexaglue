package com.example.ecommerce.infrastructure.web;

import com.example.ecommerce.application.port.driving.OrderUseCase;
import com.example.ecommerce.domain.customer.CustomerId;
import com.example.ecommerce.domain.order.Order;
import com.example.ecommerce.domain.order.OrderId;
import com.example.ecommerce.domain.order.OrderLine;

import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * REST controller serving as a driving adapter for order management operations.
 *
 * <p>This controller translates HTTP requests into calls to the {@link OrderUseCase}
 * driving port, handling string-to-identifier conversions and exposing order creation,
 * retrieval, placement, and cancellation endpoints. It uses Spring's {@code @Controller}
 * annotation for framework integration.
 *
 * <p>AUDIT VIOLATION: ddd:aggregate-boundary.
 * This controller directly accesses OrderLine, which is an internal entity
 * of the Order aggregate, bypassing the aggregate root boundary.
 */
@Controller
public class OrderController {

    private final OrderUseCase orderUseCase;

    public OrderController(OrderUseCase orderUseCase) {
        this.orderUseCase = orderUseCase;
    }

    public Order createOrder(String customerId) {
        return orderUseCase.createOrder(CustomerId.from(customerId));
    }

    public Order getOrder(String orderId) {
        return orderUseCase.getOrder(OrderId.from(orderId));
    }

    public List<Order> getCustomerOrders(String customerId) {
        return orderUseCase.getOrdersForCustomer(CustomerId.from(customerId));
    }

    /**
     * VIOLATION: Direct access to aggregate internal entity
     * Controllers should not manipulate OrderLine directly.
     */
    public void modifyOrderLine(String orderId, int lineIndex, int newQuantity) {
        Order order = orderUseCase.getOrder(OrderId.from(orderId));

        // VIOLATION: Accessing internal entity directly
        List<OrderLine> lines = order.getLines();
        if (lineIndex >= 0 && lineIndex < lines.size()) {
            OrderLine line = lines.get(lineIndex);
            // This direct manipulation bypasses the aggregate root
            OrderLine modifiedLine = line.withQuantity(newQuantity);
            // This won't actually work because the list is unmodifiable,
            // but it demonstrates the anti-pattern
        }
    }

    public void placeOrder(String orderId) {
        orderUseCase.placeOrder(OrderId.from(orderId));
    }

    public void cancelOrder(String orderId, String reason) {
        orderUseCase.cancelOrder(OrderId.from(orderId), reason);
    }
}
