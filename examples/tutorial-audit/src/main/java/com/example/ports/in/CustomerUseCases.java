package com.example.ports.in;

import com.example.domain.Address;
import com.example.domain.Customer;
import com.example.domain.CustomerId;
import com.example.domain.Email;
import java.util.Optional;

/**
 * Driving port for customer operations.
 *
 * Demonstrates best practice:
 * - Port is an interface (hexagonal:port-interface)
 * - Defines use cases that application services implement
 */
public interface CustomerUseCases {

    /**
     * Creates a new customer.
     */
    Customer createCustomer(String name, Email email);

    /**
     * Retrieves a customer by ID.
     */
    Optional<Customer> getCustomer(CustomerId id);

    /**
     * Updates a customer's shipping address.
     */
    void updateShippingAddress(CustomerId id, Address address);

    /**
     * Updates a customer's email.
     */
    void updateEmail(CustomerId id, Email email);
}
