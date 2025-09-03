package org.itinov.bankApp.domain.repository;

import org.itinov.bankApp.domain.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository interface for Customer entities.
 * Extends JpaRepository to provide CRUD operations and more.
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByKeycloakId(String keycloakId);
}
