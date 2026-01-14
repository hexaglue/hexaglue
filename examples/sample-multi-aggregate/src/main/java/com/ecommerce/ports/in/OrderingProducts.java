package com.ecommerce.ports.in;

import com.ecommerce.domain.customer.CustomerId;
import com.ecommerce.domain.order.Address;
import com.ecommerce.domain.order.Order;
import com.ecommerce.domain.order.OrderId;
import com.ecommerce.domain.product.ProductId;

import java.util.List;

/**
 * Driving port for ordering products use cases.
 */
public interface OrderingProducts {

    Order createOrder(CustomerId customerId, Address shippingAddress);

    Order addLineItem(OrderId orderId, ProductId productId, int quantity);

    Order removeLineItem(OrderId orderId, ProductId productId);

    Order confirmOrder(OrderId orderId);

    Order cancelOrder(OrderId orderId);

    Order shipOrder(OrderId orderId);

    Order completeOrder(OrderId orderId);

    Order getOrder(OrderId orderId);

    List<Order> getCustomerOrders(CustomerId customerId);
}
