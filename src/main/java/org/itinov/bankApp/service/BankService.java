package org.itinov.bankApp.service;

import org.itinov.bankApp.dto.AccountDTO;
import org.itinov.bankApp.dto.TransactionDTO;

import java.util.List;

/**
 * Service interface for banking operations including account retrieval,
 * transaction history, deposits, withdrawals, and transfers.
 */
public interface BankService {

    /**
     * Retrieves all accounts associated with a specific customer.
     *
     * @param customerId the ID of the customer
     * @return a list of AccountDTOs representing the customer's accounts
     * @throws IllegalArgumentException if the customer does not exist
     */
    List<AccountDTO> getAccountsByCustomer(Long customerId);

    /**
     * Retrieves all transactions for a specific account.
     *
     * @param accountId the ID of the account
     * @return a list of TransactionDTOs representing the account's transactions
     * @throws IllegalArgumentException if the account does not exist
     */
    List<TransactionDTO> getTransactionsByAccount(Long accountId);

    /**
     * Deposits a specified amount into an account.
     *
     * @param accountId   the ID of the account
     * @param amount      the amount to deposit
     * @param performedBy the identifier of who performed the transaction
     * @return a TransactionDTO representing the deposit transaction
     * @throws IllegalArgumentException if the account does not exist
     * @throws IllegalArgumentException if the account does not belong to the current customer
     */
    TransactionDTO deposit(Long accountId, double amount, String performedBy);

    /**
     * Withdraws a specified amount from an account.
     *
     * @param accountId   the ID of the account
     * @param amount      the amount to withdraw
     * @param performedBy the identifier of who performed the transaction
     * @return a TransactionDTO representing the withdrawal transaction
     * @throws IllegalArgumentException if the account does not exist
     * @throws IllegalArgumentException if the account does not belong to the current customer
     * @throws IllegalArgumentException if there are insufficient funds in the account
     */
    TransactionDTO withdraw(Long accountId, double amount, String performedBy);

    /**
     * Transfers a specified amount from one account to another.
     *
     * @param fromAccountId the ID of the account to transfer from
     * @param toAccountId   the ID of the account to transfer to
     * @param amount        the amount to transfer
     * @param performedBy   the identifier of who performed the transaction
     * @return a list of TransactionDTOs representing the transfer transactions
     * @throws IllegalArgumentException if either account does not exist
     * @throws IllegalArgumentException if the fromAccount does not belong to the current customer
     * @throws IllegalArgumentException if fromAccountId is the same as toAccountId
     * @throws IllegalArgumentException if there are insufficient funds in the fromAccount
     */
    List<TransactionDTO> transfer(Long fromAccountId, Long toAccountId, double amount, String performedBy);
}
