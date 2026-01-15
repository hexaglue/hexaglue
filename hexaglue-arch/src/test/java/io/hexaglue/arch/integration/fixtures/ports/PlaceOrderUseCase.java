package io.hexaglue.arch.integration.fixtures.ports;

import io.hexaglue.arch.integration.fixtures.domain.Address;
import io.hexaglue.arch.integration.fixtures.domain.CustomerId;
import io.hexaglue.arch.integration.fixtures.domain.Money;
import io.hexaglue.arch.integration.fixtures.domain.OrderId;
import java.util.List;

/**
 * Use case for placing an order (driving port).
 */
@DrivingPort
public interface PlaceOrderUseCase {

    /**
     * Places a new order.
     *
     * @param customerId the customer placing the order
     * @param lines the order line items
     * @param shippingAddress the shipping address
     * @return the created order ID
     */
    OrderId execute(CustomerId customerId, List<LineItem> lines, Address shippingAddress);

    /**
     * Line item DTO for the use case.
     */
    record LineItem(String productId, int quantity, Money unitPrice) {}
}
