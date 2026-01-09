package com.example.enterprise.payment.port.driving;

import com.example.enterprise.payment.domain.model.PaymentAggregate1;
import com.example.enterprise.payment.domain.model.PaymentAggregate1Id;
import java.util.List;

/**
 * Driving port (primary) for PaymentAggregate1 operations.
 */
public interface PaymentAggregate1Service {
    PaymentAggregate1Id create(CreatePaymentAggregate1Command command);
    PaymentAggregate1 get(PaymentAggregate1Id id);
    List<PaymentAggregate1> list();
    void update(UpdatePaymentAggregate1Command command);
    void delete(PaymentAggregate1Id id);
    void activate(PaymentAggregate1Id id);
    void complete(PaymentAggregate1Id id);
}
