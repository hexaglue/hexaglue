package com.example.ecommerce.port.driven;

import com.example.ecommerce.domain.model.Customer;
import com.example.ecommerce.domain.model.CustomerId;
import com.example.ecommerce.domain.model.Email;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for Customer persistence.
 */
public interface CustomerRepository {
    Customer save(Customer customer);

    Optional<Customer> findById(CustomerId customerId);

    Optional<Customer> findByEmail(Email email);

    List<Customer> findAll();

    void deleteById(CustomerId customerId);

    boolean existsById(CustomerId customerId);
}
