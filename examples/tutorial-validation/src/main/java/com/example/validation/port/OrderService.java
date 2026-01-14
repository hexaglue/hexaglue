package com.example.validation.port;

import com.example.validation.domain.Order;
import com.example.validation.domain.OrderId;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Order service port (driving/primary).
 *
 * <p>Classification: DRIVING port via semantic analysis (use case interface).
 *
 * <p>This interface represents the primary port that external actors
 * (REST controllers, CLI, etc.) use to interact with the order domain.
 */
public interface OrderService {

    /**
     * Creates a new order for a customer.
     *
     * @param customerId the customer identifier
     * @return the created order
     */
    Order createOrder(String customerId);

    /**
     * Adds a line item to an existing order.
     *
     * @param orderId the order identifier
     * @param productId the product identifier
     * @param quantity the quantity
     * @param unitPrice the unit price
     * @return the updated order
     */
    Order addLineItem(OrderId orderId, String productId, int quantity, BigDecimal unitPrice);

    /**
     * Confirms an order for processing.
     *
     * @param orderId the order identifier
     * @return the confirmed order
     */
    Order confirmOrder(OrderId orderId);

    /**
     * Retrieves an order by its identifier.
     *
     * @param orderId the order identifier
     * @return the order if found
     */
    Optional<Order> getOrder(OrderId orderId);

    /**
     * Lists all orders for a customer.
     *
     * @param customerId the customer identifier
     * @return list of orders
     */
    List<Order> listOrdersForCustomer(String customerId);

    /**
     * Cancels an order.
     *
     * @param orderId the order identifier
     */
    void cancelOrder(OrderId orderId);
}
