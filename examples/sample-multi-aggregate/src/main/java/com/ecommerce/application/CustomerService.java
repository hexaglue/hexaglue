package com.ecommerce.application;

import com.ecommerce.domain.customer.Customer;
import com.ecommerce.domain.customer.CustomerId;
import com.ecommerce.domain.customer.Email;
import com.ecommerce.domain.order.Address;
import com.ecommerce.ports.in.ManagingCustomers;
import com.ecommerce.ports.out.CustomerRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Application service implementing customer management use cases.
 */
public class CustomerService implements ManagingCustomers {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public Customer registerCustomer(String firstName, String lastName, Email email) {
        Customer customer = new Customer(CustomerId.generate(), firstName, lastName, email);
        return customerRepository.save(customer);
    }

    @Override
    public Customer updateProfile(CustomerId customerId, String firstName, String lastName, Email email) {
        Customer customer = findOrThrow(customerId);
        customer.updateProfile(firstName, lastName, email);
        return customerRepository.save(customer);
    }

    @Override
    public Customer updateBillingAddress(CustomerId customerId, Address billingAddress) {
        Customer customer = findOrThrow(customerId);
        customer.setBillingAddress(billingAddress);
        return customerRepository.save(customer);
    }

    @Override
    public Optional<Customer> findCustomer(CustomerId customerId) {
        return customerRepository.findById(customerId);
    }

    @Override
    public Optional<Customer> findByEmail(Email email) {
        return customerRepository.findByEmail(email);
    }

    @Override
    public List<Customer> listAllCustomers() {
        return customerRepository.findAll();
    }

    @Override
    public void deleteCustomer(CustomerId customerId) {
        Customer customer = findOrThrow(customerId);
        customerRepository.delete(customer);
    }

    private Customer findOrThrow(CustomerId customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + customerId));
    }
}
