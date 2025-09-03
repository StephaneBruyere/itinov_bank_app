package org.itinov.bankApp.config;

import org.itinov.bankApp.domain.model.Account;
import org.itinov.bankApp.domain.model.Customer;
import org.itinov.bankApp.domain.model.Transaction;
import org.itinov.bankApp.domain.model.enums.Currency;
import org.itinov.bankApp.domain.model.enums.OperationType;
import org.itinov.bankApp.domain.repository.AccountRepository;
import org.itinov.bankApp.domain.repository.CustomerRepository;
import org.itinov.bankApp.domain.repository.TransactionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Initializes demo data for the banking application.
 * Creates customers, accounts, and generates random transactions.
 */
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(CustomerRepository customerRepo,
                               AccountRepository accountRepo,
                               TransactionRepository transactionRepo) {
        return args -> {
            Random random = new Random();

            // --- Customers ---
            Customer jane = Customer.builder()
                .keycloakId("11111111-1111-1111-1111-111111111111")
                .name("Jane Smith")
                .email("jane@example.com")
                .build();

            Customer john = Customer.builder()
                .keycloakId("22222222-2222-2222-2222-222222222222")
                .name("John Doe")
                .email("john@example.com")
                .build();

            customerRepo.saveAll(List.of(jane, john));

            // --- Accounts ---
            List<Account> accounts = List.of(
                Account.builder()
                    .number("ACC-JANE-001")
                    .balance(1000.0)
                    .currency(Currency.EUR)
                    .customer(jane)
                    .overdraftLimit(-100.0)
                    .build(),
                Account.builder()
                    .number("ACC-JANE-002")
                    .balance(1500.0)
                    .currency(Currency.EUR)
                    .customer(jane)
                    .overdraftLimit(-200.0)
                    .build(),
                Account.builder()
                    .number("ACC-JOHN-001")
                    .balance(2000.0)
                    .currency(Currency.EUR)
                    .customer(john)
                    .overdraftLimit(-150.0)
                    .build()
            );

            accountRepo.saveAll(accounts);

            // --- Generate random transactions ---
            List<Transaction> allTransactions = accounts.stream()
                .flatMap(account ->
                    IntStream.range(0, 10)
                        .mapToObj(i -> {
                            OperationType type = OperationType.values()[random.nextInt(OperationType.values().length)];
                            double amount = 50 + random.nextInt(500); // 50 to 550
                            double newBalance = account.getBalance();

                            switch (type) {
                                case DEPOSIT -> newBalance += amount;
                                case WITHDRAWAL, TRANSFER -> {
                                    if (newBalance - amount >= account.getOverdraftLimit()) {
                                        newBalance -= amount;
                                    } else {
                                        return null; // skip if overdraft exceeded
                                    }
                                }
                            }

                            // update balance for the account
                            account.setBalance(newBalance);

                            //OperationType.TRANSFER
                            // For simplicity, we skip creating a second transaction for the target account
                            // In a real scenario, we'd select another account and create a corresponding transaction

                            return Transaction.builder()
                                .date(LocalDateTime.now().minusDays(10 - i))
                                .amount(amount)
                                .type(type)
                                .currency(account.getCurrency())
                                .performedBy(account.getCustomer().getName())
                                .balanceAfter(newBalance)
                                .account(account)
                                .build();
                        })
                )
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            // --- Save all transactions in batch ---
            transactionRepo.saveAll(allTransactions);
            accountRepo.saveAll(accounts); // update balances

            System.out.println("Demo data initialized with random transactions");
        };
    }
}

