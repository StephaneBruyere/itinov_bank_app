package org.itinov.bankApp.service;

import lombok.RequiredArgsConstructor;
import org.itinov.bankApp.domain.model.Account;
import org.itinov.bankApp.domain.model.Transaction;
import org.itinov.bankApp.domain.model.enums.OperationType;
import org.itinov.bankApp.domain.repository.AccountRepository;
import org.itinov.bankApp.domain.repository.TransactionRepository;
import org.itinov.bankApp.dto.AccountDTO;
import org.itinov.bankApp.dto.CustomerDTO;
import org.itinov.bankApp.dto.TransactionDTO;
import org.itinov.bankApp.mapper.BankMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * {@inheritDoc}
 */
@RequiredArgsConstructor
@Service
@Transactional
public class BankServiceImpl implements BankService {

    private final CustomerService customerService;
    private final AccountRepository accountRepo;
    private final TransactionRepository transactionRepo;
    private final BankMapper mapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AccountDTO> getAccountsByCustomer(Long customerId) {
        CustomerDTO customer = customerService.getById(customerId);
        return customer.accounts();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TransactionDTO> getTransactionsByAccount(Long accountId) {
        Account account = accountRepo.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        return account.getTransactions().stream()
            .map(mapper::toDTO)
            .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransactionDTO deposit(Long accountId, double amount, String performedBy) {
        CustomerDTO customer = customerService.getCurrentCustomer();

        Account account = accountRepo.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (customer.accounts().stream().noneMatch(accountDTO -> accountDTO.id().equals(accountId))) {
            throw new IllegalArgumentException("Account does not belong to the current customer");
        }

        account.setBalance(account.getBalance() + amount);

        Transaction tx = Transaction.builder()
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

        return mapper.toDTO(tx);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransactionDTO withdraw(Long accountId, double amount, String performedBy) {
        CustomerDTO customer = customerService.getCurrentCustomer();

        Account account = accountRepo.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (customer.accounts().stream().noneMatch(accountDTO -> accountDTO.id().equals(accountId))) {
            throw new IllegalArgumentException("Account does not belong to the current customer");
        }

        if ((account.getBalance() - amount) < account.getOverdraftLimit()) {
            throw new IllegalArgumentException("Withdrawal would exceed overdraft limit");
        }

        account.setBalance(account.getBalance() - amount);

        Transaction tx = Transaction.builder()
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

        return mapper.toDTO(tx);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TransactionDTO> transfer(Long fromAccountId, Long toAccountId, double amount, String performedBy) {
        Account from = accountRepo.findById(fromAccountId)
            .orElseThrow(() -> new IllegalArgumentException("From account not found"));

        Account to = accountRepo.findById(toAccountId)
            .orElseThrow(() -> new IllegalArgumentException("To account not found"));

        CustomerDTO customer = customerService.getCurrentCustomer();

        if (customer.accounts().stream().noneMatch(accountDTO -> accountDTO.id().equals(fromAccountId))) {
            throw new IllegalArgumentException("Account does not belong to the current customer");
        }

        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        if ((from.getBalance() - amount) < from.getOverdraftLimit()) {
            throw new IllegalArgumentException("Transfer would exceed overdraft limit");
        }

        from.setBalance(from.getBalance() - amount);
        to.setBalance(to.getBalance() + amount);

        Transaction txFrom = Transaction.builder()
            .date(LocalDateTime.now())
            .amount(amount)
            .type(OperationType.TRANSFER)
            .currency(from.getCurrency())
            .performedBy(performedBy)
            .balanceAfter(from.getBalance())
            .account(from)
            .build();

        Transaction txTo = Transaction.builder()
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

        return List.of(mapper.toDTO(txFrom), mapper.toDTO(txTo));
    }
}
