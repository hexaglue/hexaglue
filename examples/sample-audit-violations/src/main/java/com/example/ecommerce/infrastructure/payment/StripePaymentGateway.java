package com.example.ecommerce.infrastructure.payment;

import com.example.ecommerce.application.port.driven.PaymentGateway;
import com.example.ecommerce.domain.order.Money;
import com.example.ecommerce.domain.order.OrderId;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stripe implementation of the PaymentGateway port.
 */
public class StripePaymentGateway implements PaymentGateway {

    // Simulated transaction storage
    private final Map<String, PaymentStatus> transactions = new HashMap<>();

    @Override
    public PaymentResult processPayment(OrderId orderId, Money amount, String paymentMethodToken) {
        // Simulate payment processing
        if (paymentMethodToken == null || paymentMethodToken.isBlank()) {
            return PaymentResult.failure("Invalid payment method token");
        }

        // Simulate declined cards
        if (paymentMethodToken.startsWith("declined_")) {
            return PaymentResult.failure("Payment declined by issuer");
        }

        // Simulate successful payment
        String transactionId = "txn_" + UUID.randomUUID().toString().substring(0, 8);
        transactions.put(transactionId, PaymentStatus.COMPLETED);

        return PaymentResult.success(transactionId);
    }

    @Override
    public PaymentResult refundPayment(String transactionId, Money amount) {
        PaymentStatus status = transactions.get(transactionId);

        if (status == null) {
            return PaymentResult.failure("Transaction not found: " + transactionId);
        }

        if (status != PaymentStatus.COMPLETED) {
            return PaymentResult.failure("Cannot refund transaction in status: " + status);
        }

        transactions.put(transactionId, PaymentStatus.REFUNDED);
        return PaymentResult.success(transactionId);
    }

    @Override
    public PaymentStatus checkPaymentStatus(String transactionId) {
        return transactions.getOrDefault(transactionId, PaymentStatus.PENDING);
    }
}
