package com.example.ecommerce.infrastructure.persistence;

import com.example.ecommerce.application.port.driven.CustomerRepository;
import com.example.ecommerce.domain.customer.Customer;
import com.example.ecommerce.domain.customer.CustomerId;
import com.example.ecommerce.domain.customer.Email;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * JPA implementation of the CustomerRepository port.
 */
public class JpaCustomerRepository implements CustomerRepository {

    // In-memory storage for demo
    private final Map<CustomerId, Customer> storage = new HashMap<>();
    private final Map<Email, CustomerId> emailIndex = new HashMap<>();

    @Override
    public void save(Customer customer) {
        storage.put(customer.getId(), customer);
        emailIndex.put(customer.getEmail(), customer.getId());
    }

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<Customer> findByEmail(Email email) {
        CustomerId customerId = emailIndex.get(email);
        if (customerId == null) {
            return Optional.empty();
        }
        return findById(customerId);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return emailIndex.containsKey(email);
    }

    @Override
    public void delete(CustomerId id) {
        Customer customer = storage.remove(id);
        if (customer != null) {
            emailIndex.remove(customer.getEmail());
        }
    }

    @Override
    public long count() {
        return storage.size();
    }
}
