package com.example.ecommerce.port.driving;

import com.example.ecommerce.domain.model.CustomerId;
import com.example.ecommerce.domain.model.ProductId;
import com.example.ecommerce.domain.model.Address;
import java.util.List;

/**
 * Command for placing a new order.
 */
public record PlaceOrderCommand(
    CustomerId customerId,
    List<OrderLineItem> items,
    Address shippingAddress
) {
    public record OrderLineItem(ProductId productId, int quantity) {
    }
}
