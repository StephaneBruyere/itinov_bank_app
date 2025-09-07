package org.itinov.bankApp.infrastructure.repository;

import org.itinov.bankApp.infrastructure.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository interface for Account entities.
 * Extends JpaRepository to provide CRUD operations and more.
 */
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {
    /**
     * Finds all accounts associated with a specific customer ID.
     *
     * @param customerId the ID of the customer
     * @return a list of accounts belonging to the specified customer
     */
    List<AccountEntity> findByCustomerId(Long customerId);

    /**
     * Checks if an account exists with the given account ID and customer ID.
     *
     * @param accountId  the ID of the account
     * @param customerId the ID of the customer
     * @return true if an account exists with the specified IDs, false otherwise
     */
    boolean existsByIdAndCustomerId(Long accountId, Long customerId);
}
