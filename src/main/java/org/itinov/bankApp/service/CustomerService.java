package org.itinov.bankApp.service;

import org.itinov.bankApp.domain.model.Customer;

import java.util.List;

/**
 * Service interface for managing customers in the banking application.
 */
public interface CustomerService {
    /**
     * Retrieves a list of all customers.
     *
     * @return a list of CustomerDTOs representing all customers in the domain model
     */
    List<Customer> findAllCustomers();

    /**
     * Retrieves a customer by their ID.
     *
     * @param customerId the ID of the customer
     * @return a CustomerDTO representing the customer in the domain model
     * @throws IllegalArgumentException if the customer is not found
     */
    Customer getById(Long customerId);

    /**
     * Retrieves the currently authenticated customer.
     *
     * @return a Customer representing the current customer in the domain model
     * @throws IllegalArgumentException if the customer is not found
     */
    Customer getCurrentCustomer();
}
