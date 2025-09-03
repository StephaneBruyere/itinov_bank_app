package org.itinov.bankApp.domain.repository;

import org.itinov.bankApp.domain.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for Account entities.
 * Extends JpaRepository to provide CRUD operations and more.
 */
public interface AccountRepository extends JpaRepository<Account, Long> {
}
