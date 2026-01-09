package com.example.enterprise.shipping.domain.service;

import com.example.enterprise.shipping.domain.model.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Domain service for shipping calculations.
 */
public class ShippingCalculationService {
    public ShippingAmount1 calculateTotal(List<ShippingAmount1> amounts) {
        if (amounts == null || amounts.isEmpty()) {
            return ShippingAmount1.zero("USD");
        }
        ShippingAmount1 total = ShippingAmount1.zero(amounts.get(0).currency());
        for (ShippingAmount1 amount : amounts) {
            total = total.add(amount);
        }
        return total;
    }

    public BigDecimal calculatePercentage(BigDecimal value, BigDecimal percentage) {
        return value.multiply(percentage).divide(BigDecimal.valueOf(100));
    }
}
