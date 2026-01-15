package io.hexaglue.arch.integration.fixtures.ports;

import io.hexaglue.arch.integration.fixtures.domain.Money;
import io.hexaglue.arch.integration.fixtures.domain.OrderId;

/**
 * Gateway to external payment system (driven port).
 */
@DrivenPort
public interface PaymentGateway {

    /**
     * Charges a payment for an order.
     *
     * @param orderId the order ID
     * @param amount the amount to charge
     * @return the payment reference
     */
    String charge(OrderId orderId, Money amount);

    /**
     * Refunds a payment.
     *
     * @param paymentReference the payment reference to refund
     * @return true if refund succeeded
     */
    boolean refund(String paymentReference);
}
