package org.itinov.bankApp.infrastructure.repository;

import org.itinov.bankApp.infrastructure.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository interface for Transaction entities.
 * Extends JpaRepository to provide CRUD operations and more.
 */
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    /**
     * Finds all transactions associated with a specific account ID,
     * ordered by date in descending order.
     *
     * @param accountId the ID of the account
     * @return a list of transactions for the specified account, ordered by date descending
     */
    List<TransactionEntity> findByAccountIdOrderByDateDesc(Long accountId);
}
