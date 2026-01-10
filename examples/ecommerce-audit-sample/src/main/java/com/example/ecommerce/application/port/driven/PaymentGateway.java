package com.example.ecommerce.application.port.driven;

import com.example.ecommerce.domain.order.Money;
import com.example.ecommerce.domain.order.OrderId;

/**
 * Driven port for payment processing.
 */
public interface PaymentGateway {

    /**
     * Result of a payment operation.
     */
    record PaymentResult(
            String transactionId,
            boolean successful,
            String errorMessage
    ) {
        public static PaymentResult success(String transactionId) {
            return new PaymentResult(transactionId, true, null);
        }

        public static PaymentResult failure(String errorMessage) {
            return new PaymentResult(null, false, errorMessage);
        }
    }

    /**
     * Processes a payment.
     */
    PaymentResult processPayment(OrderId orderId, Money amount, String paymentMethodToken);

    /**
     * Refunds a payment.
     */
    PaymentResult refundPayment(String transactionId, Money amount);

    /**
     * Checks payment status.
     */
    PaymentStatus checkPaymentStatus(String transactionId);

    /**
     * Payment status enumeration.
     */
    enum PaymentStatus {
        PENDING,
        COMPLETED,
        FAILED,
        REFUNDED
    }
}
