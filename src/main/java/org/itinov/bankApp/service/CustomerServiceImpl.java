package org.itinov.bankApp.service;

import lombok.RequiredArgsConstructor;
import org.itinov.bankApp.domain.model.Customer;
import org.itinov.bankApp.infrastructure.repository.CustomerRepository;
import org.itinov.bankApp.mapper.BankPersistenceMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * {@inheritDoc}
 */
@RequiredArgsConstructor
@Service
@Transactional
class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepo;
    private final BankPersistenceMapper mapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Customer> findAllCustomers() {
        return customerRepo.findAll().stream()
            .map(mapper::toDomain)
            .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Customer getById(Long customerId) {
        return customerRepo.findById(customerId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Customer getCurrentCustomer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt principal = (Jwt) authentication.getPrincipal();

        String keycloakId = principal.getSubject(); // le "sub" dans le token
        return customerRepo.findByKeycloakId(keycloakId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    }
}
