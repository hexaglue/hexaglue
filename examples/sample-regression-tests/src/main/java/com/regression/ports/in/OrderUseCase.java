package com.regression.ports.in;

import com.regression.domain.customer.CustomerId;
import com.regression.domain.order.Order;
import com.regression.domain.order.OrderId;
import com.regression.domain.order.OrderLine;
import com.regression.domain.shared.Money;

import java.util.List;
import java.util.Optional;

/**
 * Driving port for order operations.
 * <p>
 * Tests H1: Interfaces with "UseCase" suffix (singular) in ports.in package
 * should be classified as DRIVING ports, not DRIVEN.
 * <p>
 * Tests M7: Return types with generics should be fully displayed.
 */
public interface OrderUseCase {

    /**
     * Creates a new order for a customer.
     */
    Order createOrder(CustomerId customerId, String currency);

    /**
     * Adds a line item to an existing order.
     */
    Order addLineToOrder(OrderId orderId, OrderLine line);

    /**
     * Applies a discount to an order.
     */
    Order applyDiscount(OrderId orderId, Money discount);

    /**
     * Confirms an order for processing.
     */
    Order confirmOrder(OrderId orderId);

    /**
     * Ships an order.
     */
    Order shipOrder(OrderId orderId);

    /**
     * Finds an order by its identifier.
     */
    Optional<Order> findOrder(OrderId id);

    /**
     * Lists all orders for a customer.
     */
    List<Order> findOrdersByCustomer(CustomerId customerId);

    /**
     * Lists all urgent orders.
     */
    List<Order> findUrgentOrders();
}
