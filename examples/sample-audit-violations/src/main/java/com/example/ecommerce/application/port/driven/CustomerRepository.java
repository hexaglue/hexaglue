package com.example.ecommerce.application.port.driven;

import com.example.ecommerce.domain.customer.Customer;
import com.example.ecommerce.domain.customer.CustomerId;
import com.example.ecommerce.domain.customer.Email;

import java.util.Optional;

/**
 * Driven port defining the persistence contract for {@link com.example.ecommerce.domain.customer.Customer} aggregates.
 *
 * <p>This repository interface abstracts the data access layer, enabling the domain
 * to persist and retrieve customers without coupling to a specific storage technology.
 * Implementations may use JPA, in-memory storage, or any other persistence mechanism.
 *
 * <p>Supports identity-based and email-based lookups, existence checks for email
 * uniqueness validation, and basic CRUD operations.
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
