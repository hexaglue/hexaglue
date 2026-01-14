package com.example.ecommerce.application.port.driven;

import com.example.ecommerce.domain.customer.Customer;
import com.example.ecommerce.domain.customer.CustomerId;
import com.example.ecommerce.domain.customer.Email;

import java.util.Optional;

/**
 * Driven port (repository interface) for Customer persistence.
 */
public interface CustomerRepository {

    /**
     * Saves a customer.
     */
    void save(Customer customer);

    /**
     * Finds a customer by ID.
     */
    Optional<Customer> findById(CustomerId id);

    /**
     * Finds a customer by email.
     */
    Optional<Customer> findByEmail(Email email);

    /**
     * Checks if an email is already registered.
     */
    boolean existsByEmail(Email email);

    /**
     * Deletes a customer.
     */
    void delete(CustomerId id);

    /**
     * Counts all customers.
     */
    long count();
}
