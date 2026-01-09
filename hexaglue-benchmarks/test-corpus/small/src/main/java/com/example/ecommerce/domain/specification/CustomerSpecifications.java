package com.example.ecommerce.domain.specification;

import com.example.ecommerce.domain.model.Customer;

/**
 * Specifications for Customer queries and business rules.
 */
public class CustomerSpecifications {

    public static Specification<Customer> hasDefaultShippingAddress() {
        return customer -> customer.getDefaultShippingAddress() != null;
    }

    public static Specification<Customer> hasPhoneNumber() {
        return customer -> customer.getPhoneNumber() != null;
    }

    public static Specification<Customer> isComplete() {
        return hasDefaultShippingAddress().and(hasPhoneNumber());
    }
}
