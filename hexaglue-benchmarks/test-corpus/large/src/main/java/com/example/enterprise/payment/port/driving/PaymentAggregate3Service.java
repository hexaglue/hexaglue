package com.example.enterprise.payment.port.driving;

import com.example.enterprise.payment.domain.model.PaymentAggregate3;
import com.example.enterprise.payment.domain.model.PaymentAggregate3Id;
import java.util.List;

/**
 * Driving port (primary) for PaymentAggregate3 operations.
 */
public interface PaymentAggregate3Service {
    PaymentAggregate3Id create(CreatePaymentAggregate3Command command);
    PaymentAggregate3 get(PaymentAggregate3Id id);
    List<PaymentAggregate3> list();
    void update(UpdatePaymentAggregate3Command command);
    void delete(PaymentAggregate3Id id);
    void activate(PaymentAggregate3Id id);
    void complete(PaymentAggregate3Id id);
}
