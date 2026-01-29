package com.example.ecommerce.application.port.driving;

import com.example.ecommerce.domain.customer.Address;
import com.example.ecommerce.domain.customer.Customer;
import com.example.ecommerce.domain.customer.CustomerId;
import com.example.ecommerce.domain.customer.Email;

/**
 * Driving port defining the customer management use cases exposed to external actors.
 *
 * <p>This interface represents the primary entry point for all customer-related
 * operations in the e-commerce platform, including registration, profile management,
 * email verification, address handling, and customer retrieval. It is implemented
 * by {@link com.example.ecommerce.application.service.CustomerApplicationService}
 * and consumed by driving adapters such as REST controllers and CLI tools.
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
