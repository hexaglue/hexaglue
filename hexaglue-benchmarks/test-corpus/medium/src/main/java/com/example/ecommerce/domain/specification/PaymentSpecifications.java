package com.example.ecommerce.domain.specification;

import com.example.ecommerce.domain.model.Payment;

/**
 * Specifications for Payment.
 */
public class PaymentSpecifications {
    public static Specification<Payment> isActive() {
        return entity -> true;
    }
}
