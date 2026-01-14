package com.example.ports.out;

import com.example.domain.Customer;
import com.example.domain.CustomerId;
import com.example.domain.Email;
import java.util.List;
import java.util.Optional;

/**
 * Driven port for customer persistence.
 *
 * Demonstrates best practices:
 * - Port is an interface (hexagonal:port-interface)
 * - Aggregate root has a repository (ddd:aggregate-repository)
 */
public interface CustomerRepository {

    /**
     * Saves a customer.
     */
    Customer save(Customer customer);

    /**
     * Finds a customer by ID.
     */
    Optional<Customer> findById(CustomerId id);

    /**
     * Finds a customer by email.
     */
    Optional<Customer> findByEmail(Email email);

    /**
     * Lists all customers.
     */
    List<Customer> findAll();

    /**
     * Deletes a customer.
     */
    void delete(Customer customer);
}
