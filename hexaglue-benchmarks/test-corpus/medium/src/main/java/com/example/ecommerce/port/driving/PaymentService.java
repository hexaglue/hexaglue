package com.example.ecommerce.port.driving;

import com.example.ecommerce.domain.model.PaymentId;
import com.example.ecommerce.domain.model.Payment;
import java.util.List;

/**
 * Driving port (primary) for payment operations.
 */
public interface PaymentService {
    PaymentId create(CreatePaymentCommand command);

    Payment getPayment(PaymentId id);

    List<Payment> getAll();

    void update(PaymentId id, UpdatePaymentCommand command);

    void delete(PaymentId id);
}
