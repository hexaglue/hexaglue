package com.example.ecommerce.application.port.driving;

import com.example.ecommerce.domain.customer.CustomerId;
import com.example.ecommerce.domain.order.Order;
import com.example.ecommerce.domain.order.OrderId;
import com.example.ecommerce.domain.product.ProductId;
import com.example.ecommerce.domain.order.Money;

import java.util.List;

/**
 * Driving port defining the order management use cases exposed to external actors.
 *
 * <p>This interface covers the full order lifecycle: creation, line item addition,
 * placement, payment confirmation, shipping, cancellation, and retrieval.
 * It is implemented by {@link com.example.ecommerce.application.service.OrderApplicationService}
 * and consumed by web controllers and other driving adapters.
 */
public interface OrderUseCase {

    /**
     * Creates a new order for a customer.
     */
    Order createOrder(CustomerId customerId);

    /**
     * Adds a product to an existing order.
     */
    void addLineToOrder(OrderId orderId, ProductId productId, String productName,
                        int quantity, Money unitPrice);

    /**
     * Places an order, transitioning it from draft to pending payment.
     */
    void placeOrder(OrderId orderId);

    /**
     * Confirms payment for an order.
     */
    void confirmPayment(OrderId orderId);

    /**
     * Ships an order with tracking information.
     */
    void shipOrder(OrderId orderId, String trackingNumber, String carrier);

    /**
     * Cancels an order.
     */
    void cancelOrder(OrderId orderId, String reason);

    /**
     * Retrieves an order by ID.
     */
    Order getOrder(OrderId orderId);

    /**
     * Retrieves all orders for a customer.
     */
    List<Order> getOrdersForCustomer(CustomerId customerId);
}
