package com.example.ecommerce.application.port.driving;

import com.example.ecommerce.domain.customer.Address;
import com.example.ecommerce.domain.customer.Customer;
import com.example.ecommerce.domain.customer.CustomerId;
import com.example.ecommerce.domain.customer.Email;

/**
 * Driving port (use case interface) for customer operations.
 * This is a proper port - it's an interface.
 */
public interface CustomerUseCase {

    /**
     * Registers a new customer.
     */
    Customer registerCustomer(String firstName, String lastName, Email email);

    /**
     * Updates customer profile.
     */
    void updateCustomerProfile(CustomerId customerId, String firstName, String lastName);

    /**
     * Changes customer email.
     */
    void changeEmail(CustomerId customerId, Email newEmail);

    /**
     * Adds an address to customer profile.
     */
    void addAddress(CustomerId customerId, Address address);

    /**
     * Sets the default shipping address.
     */
    void setDefaultShippingAddress(CustomerId customerId, Address address);

    /**
     * Verifies customer email.
     */
    void verifyEmail(CustomerId customerId);

    /**
     * Retrieves a customer by ID.
     */
    Customer getCustomer(CustomerId customerId);

    /**
     * Retrieves a customer by email.
     */
    Customer getCustomerByEmail(Email email);
}
