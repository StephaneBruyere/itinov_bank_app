package org.itinov.bankApp.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.itinov.bankApp.domain.model.Customer;
import org.itinov.bankApp.dto.CustomerDTO;
import org.itinov.bankApp.mapper.BankAPIMapper;
import org.itinov.bankApp.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for customer-related operations.
 * Some endpoints are public, others require the user to be authenticated and have a keycloak 'customer' role.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class CustomerController {

    private final CustomerService customerService;
    private final BankAPIMapper mapper;

    /**
     * Returns all customers with their accounts and transactions
     */
    @GetMapping("/public/customers")
    @Operation(summary = "Get all customers with their accounts and transactions")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of customers returned")
    })
    public ResponseEntity<List<CustomerDTO>> getAllCustomersFull() {
        List<Customer> customers = customerService.findAllCustomers();
        return ResponseEntity.ok(mapper.toCustomerDTOs(customers));
    }

    @PreAuthorize("isAuthenticated() and hasRole('customer')")
    @GetMapping("/customer")
    @Operation(summary = "Get authenticated customer's id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Current customer's id"),
        @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<Long> getCurrentCustomerId() {
        Customer currentCustomer = customerService.getCurrentCustomer();
        return ResponseEntity.ok(mapper.toDTO(currentCustomer).id());
    }
}
