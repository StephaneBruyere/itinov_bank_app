package org.itinov.bankApp.service;

import org.itinov.bankApp.dto.CustomerDTO;

import java.util.List;

/**
 * Service interface for managing customers in the banking application.
 */
public interface CustomerService {
    /**
     * Retrieves a list of all customers.
     *
     * @return a list of CustomerDTOs representing all customers
     */
    List<CustomerDTO> findAllCustomers();

    /**
     * Retrieves a customer by their ID.
     *
     * @param customerId the ID of the customer
     * @return a CustomerDTO representing the customer
     * @throws IllegalArgumentException if the customer is not found
     */
    CustomerDTO getById(Long customerId);

    /**
     * Retrieves the currently authenticated customer.
     *
     * @return a CustomerDTO representing the current customer
     * @throws IllegalArgumentException if the customer is not found
     */
    CustomerDTO getCurrentCustomer();
}
