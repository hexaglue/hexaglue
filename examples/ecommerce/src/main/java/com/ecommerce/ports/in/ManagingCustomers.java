package com.ecommerce.ports.in;

import com.ecommerce.domain.customer.Customer;
import com.ecommerce.domain.customer.CustomerId;
import com.ecommerce.domain.customer.Email;
import com.ecommerce.domain.order.Address;

import java.util.List;
import java.util.Optional;

/**
 * Driving port for customer management use cases.
 */
public interface ManagingCustomers {

    Customer registerCustomer(String firstName, String lastName, Email email);

    Customer updateProfile(CustomerId customerId, String firstName, String lastName, Email email);

    Customer updateBillingAddress(CustomerId customerId, Address billingAddress);

    Optional<Customer> findCustomer(CustomerId customerId);

    Optional<Customer> findByEmail(Email email);

    List<Customer> listAllCustomers();

    void deleteCustomer(CustomerId customerId);
}
