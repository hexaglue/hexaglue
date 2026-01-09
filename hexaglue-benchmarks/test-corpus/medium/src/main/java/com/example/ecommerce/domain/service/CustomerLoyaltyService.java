package com.example.ecommerce.domain.service;

import com.example.ecommerce.domain.model.Customer;

/**
 * Domain service for customer loyalty calculations.
 */
public class CustomerLoyaltyService {

    public enum LoyaltyTier {
        BRONZE,
        SILVER,
        GOLD,
        PLATINUM
    }

    public LoyaltyTier calculateLoyaltyTier(Customer customer, int totalOrders) {
        if (totalOrders >= 50) {
            return LoyaltyTier.PLATINUM;
        } else if (totalOrders >= 20) {
            return LoyaltyTier.GOLD;
        } else if (totalOrders >= 5) {
            return LoyaltyTier.SILVER;
        } else {
            return LoyaltyTier.BRONZE;
        }
    }

    public double getDiscountPercentage(LoyaltyTier tier) {
        return switch (tier) {
            case BRONZE -> 0.0;
            case SILVER -> 5.0;
            case GOLD -> 10.0;
            case PLATINUM -> 15.0;
        };
    }
}
