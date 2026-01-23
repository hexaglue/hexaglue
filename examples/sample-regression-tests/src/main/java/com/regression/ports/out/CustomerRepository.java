package com.regression.ports.out;

import com.regression.domain.customer.Customer;
import com.regression.domain.customer.CustomerId;
import com.regression.domain.customer.Email;

import java.util.List;
import java.util.Optional;

/**
 * Driven port (Repository) for Customer persistence.
 * <p>
 * Tests M12: existsByEmail(Email) returns boolean and must generate
 * a working adapter implementation.
 * <p>
 * Tests M7: Return types List&lt;Customer&gt; and Optional&lt;Customer&gt;
 * should be fully displayed in documentation.
 */
public interface CustomerRepository {

    /**
     * Saves a customer.
     */
    Customer save(Customer customer);

    /**
     * Finds a customer by ID using standard naming.
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
     * Checks if a customer with the given email exists.
     * <p>
     * Tests M12: boolean return type adapter generation.
     */
    boolean existsByEmail(Email email);

    /**
     * Deletes a customer.
     */
    void delete(Customer customer);
}
