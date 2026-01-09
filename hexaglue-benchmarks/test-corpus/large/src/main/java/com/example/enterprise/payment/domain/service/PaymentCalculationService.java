package com.example.enterprise.payment.domain.service;

import com.example.enterprise.payment.domain.model.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Domain service for payment calculations.
 */
public class PaymentCalculationService {
    public PaymentAmount1 calculateTotal(List<PaymentAmount1> amounts) {
        if (amounts == null || amounts.isEmpty()) {
            return PaymentAmount1.zero("USD");
        }
        PaymentAmount1 total = PaymentAmount1.zero(amounts.get(0).currency());
        for (PaymentAmount1 amount : amounts) {
            total = total.add(amount);
        }
        return total;
    }

    public BigDecimal calculatePercentage(BigDecimal value, BigDecimal percentage) {
        return value.multiply(percentage).divide(BigDecimal.valueOf(100));
    }
}
