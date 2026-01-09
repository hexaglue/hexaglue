package com.example.ecommerce.application;

import com.example.ecommerce.domain.model.*;
import com.example.ecommerce.port.driven.CustomerRepository;
import com.example.ecommerce.port.driven.EventPublisher;
import com.example.ecommerce.port.driving.CustomerService;
import com.example.ecommerce.port.driving.RegisterCustomerCommand;
import com.example.ecommerce.port.driving.UpdateContactInfoCommand;
import com.example.ecommerce.port.driving.UpdateShippingAddressCommand;
import java.util.List;

/**
 * Use case implementation for managing customers.
 */
public class ManageCustomerUseCase implements CustomerService {
    private final CustomerRepository customerRepository;
    private final EventPublisher eventPublisher;

    public ManageCustomerUseCase(
        CustomerRepository customerRepository,
        EventPublisher eventPublisher
    ) {
        this.customerRepository = customerRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public CustomerId registerCustomer(RegisterCustomerCommand command) {
        Email email = new Email(command.email());

        // Check if customer already exists
        if (customerRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("Customer with email already exists: " + email.value());
        }

        // Create customer
        Address shippingAddress = new Address(
            command.shippingAddress().street(),
            command.shippingAddress().city(),
            command.shippingAddress().postalCode(),
            command.shippingAddress().country()
        );

        Customer customer = new Customer(
            CustomerId.generate(),
            command.firstName(),
            command.lastName(),
            email,
            new PhoneNumber(command.phoneNumber()),
            shippingAddress
        );

        Customer savedCustomer = customerRepository.save(customer);

        // Publish event
        eventPublisher.publish(new CustomerRegisteredEvent(savedCustomer.getId(), savedCustomer.getEmail()));

        return savedCustomer.getId();
    }

    @Override
    public Customer getCustomer(CustomerId customerId) {
        return customerRepository.findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
    }

    @Override
    public Customer findByEmail(Email email) {
        return customerRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found with email: " + email.value()));
    }

    @Override
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @Override
    public void updateContactInfo(CustomerId customerId, UpdateContactInfoCommand command) {
        Customer customer = getCustomer(customerId);

        Email newEmail = command.email() != null ? new Email(command.email()) : null;
        PhoneNumber newPhone = command.phoneNumber() != null ? new PhoneNumber(command.phoneNumber()) : null;

        customer.updateContactInfo(newEmail, newPhone);
        customerRepository.save(customer);

        eventPublisher.publish(new CustomerContactInfoUpdatedEvent(customerId));
    }

    @Override
    public void updateShippingAddress(CustomerId customerId, UpdateShippingAddressCommand command) {
        Customer customer = getCustomer(customerId);

        Address newAddress = new Address(
            command.street(),
            command.city(),
            command.postalCode(),
            command.country()
        );

        customer.updateDefaultShippingAddress(newAddress);
        customerRepository.save(customer);

        eventPublisher.publish(new CustomerAddressUpdatedEvent(customerId));
    }

    private record CustomerRegisteredEvent(CustomerId customerId, Email email) implements EventPublisher.DomainEvent {
        @Override
        public String eventType() {
            return "CustomerRegistered";
        }
    }

    private record CustomerContactInfoUpdatedEvent(CustomerId customerId) implements EventPublisher.DomainEvent {
        @Override
        public String eventType() {
            return "CustomerContactInfoUpdated";
        }
    }

    private record CustomerAddressUpdatedEvent(CustomerId customerId) implements EventPublisher.DomainEvent {
        @Override
        public String eventType() {
            return "CustomerAddressUpdated";
        }
    }
}
