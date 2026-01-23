package com.regression.application;

import com.regression.domain.customer.Customer;
import com.regression.domain.customer.CustomerId;
import com.regression.domain.customer.Email;
import com.regression.ports.in.CustomerUseCases;
import com.regression.ports.out.CustomerRepository;

import java.util.List;
import java.util.Optional;

/**
 * Application service implementing customer use cases.
 */
public class CustomerService implements CustomerUseCases {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public Customer registerCustomer(String name, Email email) {
        if (customerRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Customer with email " + email + " already exists");
        }
        var customer = Customer.create(name, email);
        return customerRepository.save(customer);
    }

    @Override
    public Optional<Customer> findCustomer(CustomerId id) {
        return customerRepository.findById(id);
    }

    @Override
    public List<Customer> listAllCustomers() {
        return customerRepository.findAll();
    }

    @Override
    public void deactivateCustomer(CustomerId id) {
        customerRepository.findById(id)
                .map(Customer::deactivate)
                .ifPresent(customerRepository::save);
    }

    @Override
    public Customer changeCustomerEmail(CustomerId id, Email newEmail) {
        if (customerRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("Email " + newEmail + " is already in use");
        }
        return customerRepository.findById(id)
                .map(c -> c.changeEmail(newEmail))
                .map(customerRepository::save)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
    }
}
