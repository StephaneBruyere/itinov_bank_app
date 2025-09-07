package org.itinov.bankApp.infrastructure.repository;

import org.itinov.bankApp.infrastructure.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository interface for Customer entities.
 * Extends JpaRepository to provide CRUD operations and more.
 */
public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {
    Optional<CustomerEntity> findByKeycloakId(String keycloakId);
}
