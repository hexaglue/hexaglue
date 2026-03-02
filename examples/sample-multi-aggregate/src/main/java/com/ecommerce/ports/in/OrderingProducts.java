package com.ecommerce.ports.in;

import com.ecommerce.domain.customer.CustomerId;
import com.ecommerce.domain.exception.BusinessRuleViolationException;
import com.ecommerce.domain.exception.ResourceNotFoundException;
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

    Order addLineItem(OrderId orderId, ProductId productId, int quantity)
            throws ResourceNotFoundException, BusinessRuleViolationException;

    Order removeLineItem(OrderId orderId, ProductId productId)
            throws ResourceNotFoundException, BusinessRuleViolationException;

    Order confirmOrder(OrderId orderId)
            throws ResourceNotFoundException, BusinessRuleViolationException;

    Order cancelOrder(OrderId orderId)
            throws ResourceNotFoundException, BusinessRuleViolationException;

    Order shipOrder(OrderId orderId)
            throws ResourceNotFoundException, BusinessRuleViolationException;

    Order completeOrder(OrderId orderId)
            throws ResourceNotFoundException, BusinessRuleViolationException;

    Order getOrder(OrderId orderId) throws ResourceNotFoundException;

    List<Order> getCustomerOrders(CustomerId customerId);
}
