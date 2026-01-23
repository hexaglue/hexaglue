package com.regression.ports.in;

import com.regression.domain.customer.Customer;
import com.regression.domain.customer.CustomerId;
import com.regression.domain.customer.Email;

import java.util.List;
import java.util.Optional;

/**
 * Driving port for customer operations.
 * <p>
 * Tests H1: Interfaces with "UseCases" suffix (plural) in ports.in package
 * should be classified as DRIVING ports, not DRIVEN.
 * <p>
 * Tests M7: Return types like List&lt;Customer&gt; and Optional&lt;Customer&gt;
 * should display fully in generated documentation (not truncated).
 */
public interface CustomerUseCases {

    /**
     * Registers a new customer.
     */
    Customer registerCustomer(String name, Email email);

    /**
     * Finds a customer by their identifier.
     */
    Optional<Customer> findCustomer(CustomerId id);

    /**
     * Lists all customers.
     */
    List<Customer> listAllCustomers();

    /**
     * Deactivates a customer account.
     */
    void deactivateCustomer(CustomerId id);

    /**
     * Changes customer email address.
     */
    Customer changeCustomerEmail(CustomerId id, Email newEmail);
}
