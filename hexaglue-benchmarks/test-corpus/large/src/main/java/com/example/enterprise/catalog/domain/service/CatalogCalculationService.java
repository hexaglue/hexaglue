package com.example.enterprise.catalog.domain.service;

import com.example.enterprise.catalog.domain.model.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Domain service for catalog calculations.
 */
public class CatalogCalculationService {
    public CatalogAmount1 calculateTotal(List<CatalogAmount1> amounts) {
        if (amounts == null || amounts.isEmpty()) {
            return CatalogAmount1.zero("USD");
        }
        CatalogAmount1 total = CatalogAmount1.zero(amounts.get(0).currency());
        for (CatalogAmount1 amount : amounts) {
            total = total.add(amount);
        }
        return total;
    }

    public BigDecimal calculatePercentage(BigDecimal value, BigDecimal percentage) {
        return value.multiply(percentage).divide(BigDecimal.valueOf(100));
    }
}
