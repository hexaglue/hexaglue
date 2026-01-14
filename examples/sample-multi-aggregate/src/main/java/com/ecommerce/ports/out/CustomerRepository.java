package com.ecommerce.ports.out;

import com.ecommerce.domain.customer.Customer;
import com.ecommerce.domain.customer.CustomerId;
import com.ecommerce.domain.customer.Email;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for Customer aggregate persistence.
 */
public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findById(CustomerId id);

    Optional<Customer> findByEmail(Email email);

    List<Customer> findAll();

    void delete(Customer customer);

    boolean existsByEmail(Email email);
}
