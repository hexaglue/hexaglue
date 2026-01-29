package com.example.ecommerce.application.service;

import com.example.ecommerce.application.port.driving.CustomerUseCase;
import com.example.ecommerce.application.port.driven.CustomerRepository;
import com.example.ecommerce.application.port.driven.NotificationService;
import com.example.ecommerce.domain.customer.Address;
import com.example.ecommerce.domain.customer.Customer;
import com.example.ecommerce.domain.customer.CustomerId;
import com.example.ecommerce.domain.customer.Email;

/**
 * Application service orchestrating customer management use cases.
 *
 * <p>This service implements the {@link CustomerUseCase} driving port and coordinates
 * between the domain layer and driven ports (persistence and notifications). It handles
 * customer registration with email uniqueness validation, profile updates, email changes
 * with re-verification, address management, and customer lookup operations.
 *
 * <p>This service properly follows hexagonal architecture by depending only on port
 * interfaces, not on concrete infrastructure implementations.
 */
public class CustomerApplicationService implements CustomerUseCase {

    private final CustomerRepository customerRepository;
    private final NotificationService notificationService;

    public CustomerApplicationService(CustomerRepository customerRepository,
                                      NotificationService notificationService) {
        this.customerRepository = customerRepository;
        this.notificationService = notificationService;
    }

    @Override
    public Customer registerCustomer(String firstName, String lastName, Email email) {
        if (customerRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered: " + email);
        }

        Customer customer = Customer.create(firstName, lastName, email);
        customerRepository.save(customer);

        // Send welcome email
        notificationService.sendEmail(email, "Welcome!",
                "Welcome to our store, " + firstName + "!");

        return customer;
    }

    @Override
    public void updateCustomerProfile(CustomerId customerId, String firstName, String lastName) {
        Customer customer = getCustomer(customerId);
        customer.updateName(firstName, lastName);
        customerRepository.save(customer);
    }

    @Override
    public void changeEmail(CustomerId customerId, Email newEmail) {
        if (customerRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("Email already in use: " + newEmail);
        }

        Customer customer = getCustomer(customerId);
        customer.changeEmail(newEmail);
        customerRepository.save(customer);

        // Send verification email
        notificationService.sendEmail(newEmail, "Verify your email",
                "Please verify your new email address.");
    }

    @Override
    public void addAddress(CustomerId customerId, Address address) {
        Customer customer = getCustomer(customerId);
        customer.addAddress(address);
        customerRepository.save(customer);
    }

    @Override
    public void setDefaultShippingAddress(CustomerId customerId, Address address) {
        Customer customer = getCustomer(customerId);
        customer.setDefaultShippingAddress(address);
        customerRepository.save(customer);
    }

    @Override
    public void verifyEmail(CustomerId customerId) {
        Customer customer = getCustomer(customerId);
        customer.verifyEmail();
        customerRepository.save(customer);
    }

    @Override
    public Customer getCustomer(CustomerId customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
    }

    @Override
    public Customer getCustomerByEmail(Email email) {
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with email: " + email));
    }
}
