package org.itinov.bankApp.service;

import lombok.RequiredArgsConstructor;
import org.itinov.bankApp.domain.repository.CustomerRepository;
import org.itinov.bankApp.dto.CustomerDTO;
import org.itinov.bankApp.mapper.BankMapper;
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
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepo;
    private final BankMapper mapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CustomerDTO> findAllCustomers() {
        return customerRepo.findAll().stream()
            .map(mapper::toDTO)
            .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CustomerDTO getById(Long customerId) {
        return customerRepo.findById(customerId)
            .map(mapper::toDTO)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CustomerDTO getCurrentCustomer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt principal = (Jwt) authentication.getPrincipal();

        String keycloakId = principal.getSubject(); // le "sub" dans le token
        return customerRepo.findByKeycloakId(keycloakId)
            .map(mapper::toDTO)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    }
}
