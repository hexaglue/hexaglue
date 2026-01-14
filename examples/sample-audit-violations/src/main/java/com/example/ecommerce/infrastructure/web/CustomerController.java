package com.example.ecommerce.infrastructure.web;

import com.example.ecommerce.application.port.driving.CustomerUseCase;
import com.example.ecommerce.domain.customer.Address;
import com.example.ecommerce.domain.customer.Customer;
import com.example.ecommerce.domain.customer.CustomerId;
import com.example.ecommerce.domain.customer.Email;

/**
 * REST controller for customer operations.
 */
public class CustomerController {

    private final CustomerUseCase customerUseCase;

    public CustomerController(CustomerUseCase customerUseCase) {
        this.customerUseCase = customerUseCase;
    }

    public Customer register(String firstName, String lastName, String email) {
        return customerUseCase.registerCustomer(firstName, lastName, new Email(email));
    }

    public Customer getCustomer(String customerId) {
        return customerUseCase.getCustomer(CustomerId.from(customerId));
    }

    public void updateProfile(String customerId, String firstName, String lastName) {
        customerUseCase.updateCustomerProfile(CustomerId.from(customerId), firstName, lastName);
    }

    public void addAddress(String customerId, String street, String city,
                          String postalCode, String country) {
        Address address = new Address(street, city, postalCode, country);
        customerUseCase.addAddress(CustomerId.from(customerId), address);
    }

    public void verifyEmail(String customerId) {
        customerUseCase.verifyEmail(CustomerId.from(customerId));
    }
}
