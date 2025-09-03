package org.itinov.bankApp.domain.repository;

import org.itinov.bankApp.domain.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for Transaction entities.
 * Extends JpaRepository to provide CRUD operations and more.
 */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
