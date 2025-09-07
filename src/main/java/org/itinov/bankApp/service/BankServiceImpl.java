package org.itinov.bankApp.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.itinov.bankApp.domain.model.Account;
import org.itinov.bankApp.domain.model.Customer;
import org.itinov.bankApp.domain.model.Transaction;
import org.itinov.bankApp.domain.enums.OperationType;
import org.itinov.bankApp.infrastructure.entity.AccountEntity;
import org.itinov.bankApp.infrastructure.entity.TransactionEntity;
import org.itinov.bankApp.infrastructure.repository.AccountRepository;
import org.itinov.bankApp.infrastructure.repository.TransactionRepository;
import org.itinov.bankApp.mapper.BankPersistenceMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * {@inheritDoc}
 */
@RequiredArgsConstructor
@Service
@Transactional
class BankServiceImpl implements BankService {

    private final CustomerService customerService;
    private final AccountRepository accountRepo;
    private final TransactionRepository transactionRepo;
    private final BankPersistenceMapper mapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Account> getAccountsByCustomer(Long customerId) {
        Customer currentCustomer = customerService.getCurrentCustomer();
        if (!Objects.equals(currentCustomer.id(), customerId)) {
            throw new AccessDeniedException("You are not allowed to access accounts of another customer");
        }
        return accountRepo.findByCustomerId(customerId).stream()
            .map(mapper::toDomain)
            .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Transaction> getTransactionsByAccount(Long accountId) {
        Customer currentCustomer = customerService.getCurrentCustomer();
        // Vérifie que le compte appartient au client courant
        if (!accountRepo.existsByIdAndCustomerId(accountId, currentCustomer.id())) {
            throw new AccessDeniedException("You are not allowed to access this account's transactions");
        }
        return transactionRepo.findByAccountIdOrderByDateDesc(accountId).stream()
            .map(mapper::toDomain)
            .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Transaction deposit(Long accountId, double amount, String performedBy) {

        AccountEntity account = loadAndValidateOwnedAccount(accountId, amount);

        account.setBalance(account.getBalance() + amount);

        TransactionEntity tx = org.itinov.bankApp.infrastructure.entity.TransactionEntity.builder()
            .date(LocalDateTime.now())
            .amount(amount)
            .type(OperationType.DEPOSIT)
            .currency(account.getCurrency())
            .performedBy(performedBy)
            .balanceAfter(account.getBalance())
            .account(account)
            .build();

        transactionRepo.save(tx);
        accountRepo.save(account);

        return mapper.toDomain(tx);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Transaction withdraw(Long accountId, double amount, String performedBy) {

        AccountEntity account = loadAndValidateOwnedAccount(accountId, amount);

        if ((account.getBalance() - amount) < account.getOverdraftLimit()) {
            throw new IllegalArgumentException("Withdrawal would exceed overdraft limit");
        }

        account.setBalance(account.getBalance() - amount);

        TransactionEntity tx = org.itinov.bankApp.infrastructure.entity.TransactionEntity.builder()
            .date(LocalDateTime.now())
            .amount(amount)
            .type(OperationType.WITHDRAWAL)
            .currency(account.getCurrency())
            .performedBy(performedBy)
            .balanceAfter(account.getBalance())
            .account(account)
            .build();

        transactionRepo.save(tx);
        accountRepo.save(account);

        return mapper.toDomain(tx);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Transaction> transfer(Long fromAccountId, Long toAccountId, double amount, String performedBy) {
        AccountEntity from = loadAndValidateOwnedAccount(fromAccountId, amount);

        AccountEntity to = accountRepo.findById(toAccountId)
            .orElseThrow(() -> new EntityNotFoundException("To account not found"));

        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        if ((from.getBalance() - amount) < from.getOverdraftLimit()) {
            throw new IllegalArgumentException("Transfer would exceed overdraft limit");
        }

        from.setBalance(from.getBalance() - amount);
        to.setBalance(to.getBalance() + amount);

        TransactionEntity txFrom = org.itinov.bankApp.infrastructure.entity.TransactionEntity.builder()
            .date(LocalDateTime.now())
            .amount(amount)
            .type(OperationType.TRANSFER)
            .currency(from.getCurrency())
            .performedBy(performedBy)
            .balanceAfter(from.getBalance())
            .account(from)
            .build();

        TransactionEntity txTo = org.itinov.bankApp.infrastructure.entity.TransactionEntity.builder()
            .date(LocalDateTime.now())
            .amount(amount)
            .type(OperationType.TRANSFER)
            .currency(from.getCurrency())
            .performedBy(performedBy)
            .balanceAfter(to.getBalance())
            .account(to)
            .build();

        transactionRepo.saveAll(List.of(txFrom, txTo));
        accountRepo.saveAll(List.of(from, to));

        return List.of(mapper.toDomain(txFrom), mapper.toDomain(txTo));
    }

    /**
     * Loads an account by ID and validates that it belongs to the current customer
     * and that the amount is positive.
     *
     * @param accountId the ID of the account to load
     * @param amount    the amount for the operation (must be positive)
     * @return the loaded Account entity
     * @throws EntityNotFoundException  if the account does not exist
     * @throws AccessDeniedException    if the account does not belong to the current customer
     * @throws IllegalArgumentException if the amount is not positive
     */
    private AccountEntity loadAndValidateOwnedAccount(Long accountId, double amount) {
        AccountEntity account = accountRepo.findById(accountId)
            .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        Customer customer = customerService.getCurrentCustomer();

        if (!account.getCustomer().getId().equals(customer.id())) {
            throw new AccessDeniedException("Account does not belong to the current customer");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        return account;
    }
}
