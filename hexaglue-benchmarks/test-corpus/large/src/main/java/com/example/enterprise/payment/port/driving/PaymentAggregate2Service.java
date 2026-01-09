package com.example.enterprise.payment.port.driving;

import com.example.enterprise.payment.domain.model.PaymentAggregate2;
import com.example.enterprise.payment.domain.model.PaymentAggregate2Id;
import java.util.List;

/**
 * Driving port (primary) for PaymentAggregate2 operations.
 */
public interface PaymentAggregate2Service {
    PaymentAggregate2Id create(CreatePaymentAggregate2Command command);
    PaymentAggregate2 get(PaymentAggregate2Id id);
    List<PaymentAggregate2> list();
    void update(UpdatePaymentAggregate2Command command);
    void delete(PaymentAggregate2Id id);
    void activate(PaymentAggregate2Id id);
    void complete(PaymentAggregate2Id id);
}
