package com.example.enterprise.supplier.domain.service;

import com.example.enterprise.supplier.domain.model.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Domain service for supplier calculations.
 */
public class SupplierCalculationService {
    public SupplierAmount1 calculateTotal(List<SupplierAmount1> amounts) {
        if (amounts == null || amounts.isEmpty()) {
            return SupplierAmount1.zero("USD");
        }
        SupplierAmount1 total = SupplierAmount1.zero(amounts.get(0).currency());
        for (SupplierAmount1 amount : amounts) {
            total = total.add(amount);
        }
        return total;
    }

    public BigDecimal calculatePercentage(BigDecimal value, BigDecimal percentage) {
        return value.multiply(percentage).divide(BigDecimal.valueOf(100));
    }
}
