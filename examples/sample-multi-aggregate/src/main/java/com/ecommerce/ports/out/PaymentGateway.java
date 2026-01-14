package com.ecommerce.ports.out;

import com.ecommerce.domain.order.Money;
import com.ecommerce.domain.order.OrderId;

/**
 * External payment gateway port (driven port).
 */
public interface PaymentGateway {

    PaymentResult processPayment(OrderId orderId, Money amount, String paymentMethod);

    PaymentResult refund(String transactionId, Money amount);

    PaymentStatus checkStatus(String transactionId);

    record PaymentResult(boolean success, String transactionId, String message) {
        public static PaymentResult successful(String transactionId) {
            return new PaymentResult(true, transactionId, "Payment processed successfully");
        }

        public static PaymentResult failed(String message) {
            return new PaymentResult(false, null, message);
        }
    }

    enum PaymentStatus {
        PENDING, COMPLETED, FAILED, REFUNDED
    }
}
