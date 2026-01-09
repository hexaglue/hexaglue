package com.example.ecommerce.port.driving;

import com.example.ecommerce.domain.model.OrderId;
import com.example.ecommerce.domain.model.CustomerId;
import com.example.ecommerce.domain.model.Order;
import java.util.List;

/**
 * Driving port (primary) for order operations.
 */
public interface OrderService {
    OrderId placeOrder(PlaceOrderCommand command);

    Order getOrder(OrderId orderId);

    List<Order> getCustomerOrders(CustomerId customerId);

    void confirmOrder(OrderId orderId);

    void shipOrder(OrderId orderId);

    void cancelOrder(OrderId orderId);
}
