package com.example.ecommerce.port.driving;

import com.example.ecommerce.domain.model.CustomerId;
import com.example.ecommerce.domain.model.Customer;
import com.example.ecommerce.domain.model.Email;
import java.util.List;

/**
 * Driving port (primary) for customer operations.
 */
public interface CustomerService {
    CustomerId registerCustomer(RegisterCustomerCommand command);

    Customer getCustomer(CustomerId customerId);

    Customer findByEmail(Email email);

    List<Customer> getAllCustomers();

    void updateContactInfo(CustomerId customerId, UpdateContactInfoCommand command);

    void updateShippingAddress(CustomerId customerId, UpdateShippingAddressCommand command);
}
