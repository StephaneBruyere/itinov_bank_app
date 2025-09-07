package org.itinov.bankApp.repository;

import org.itinov.bankApp.infrastructure.entity.AccountEntity;
import org.itinov.bankApp.infrastructure.entity.CustomerEntity;
import org.itinov.bankApp.infrastructure.entity.TransactionEntity;
import org.itinov.bankApp.domain.enums.Currency;
import org.itinov.bankApp.domain.enums.OperationType;
import org.itinov.bankApp.infrastructure.repository.AccountRepository;
import org.itinov.bankApp.infrastructure.repository.CustomerRepository;
import org.itinov.bankApp.infrastructure.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private CustomerEntity createCustomer(String keycloakId) {
        CustomerEntity c = CustomerEntity.builder().name("Jane").email("jane@example.com").keycloakId(keycloakId).build();
        return customerRepository.save(c);
    }

    private AccountEntity createAccount(CustomerEntity customer, String number) {
        AccountEntity a = AccountEntity.builder()
                .number(number)
                .balance(0)
                .overdraftLimit(100)
                .currency(Currency.EUR)
                .customer(customer)
                .build();
        return accountRepository.save(a);
    }

    private TransactionEntity createTx(AccountEntity account, double amount, OperationType type, LocalDateTime date, double balanceAfter) {
        TransactionEntity t = TransactionEntity.builder()
                .account(account)
                .amount(amount)
                .type(type)
                .currency(Currency.EUR)
                .performedBy("tester")
                .date(date)
                .balanceAfter(balanceAfter)
                .build();
        return transactionRepository.save(t);
    }

    @Test
    void findByAccountIdOrderByDateDesc_shouldReturnTransactionsOrderedDesc() {
        CustomerEntity c = createCustomer("kc-tx");
        AccountEntity a = createAccount(c, "ACC-T");

        TransactionEntity t1 = createTx(a, 10, OperationType.DEPOSIT, LocalDateTime.now().minusDays(2), 10);
        TransactionEntity t2 = createTx(a, 5, OperationType.WITHDRAWAL, LocalDateTime.now().minusDays(1), 5);
        TransactionEntity t3 = createTx(a, 20, OperationType.DEPOSIT, LocalDateTime.now(), 25);

        List<TransactionEntity> results = transactionRepository.findByAccountIdOrderByDateDesc(a.getId());

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getId()).isEqualTo(t3.getId());
        assertThat(results.get(1).getId()).isEqualTo(t2.getId());
        assertThat(results.get(2).getId()).isEqualTo(t1.getId());
    }

    @Test
    void save_shouldPersistTransactionWithAccountRelation() {
        CustomerEntity c = createCustomer("kc-tx2");
        AccountEntity a = createAccount(c, "ACC-T2");
        TransactionEntity saved = createTx(a, 42, OperationType.DEPOSIT, LocalDateTime.now(), 42);

        List<TransactionEntity> byAccount = transactionRepository.findByAccountIdOrderByDateDesc(a.getId());
        assertThat(byAccount).hasSize(1);
        TransactionEntity fetched = byAccount.getFirst();
        assertThat(fetched.getAccount().getId()).isEqualTo(a.getId());
        assertThat(fetched.getAmount()).isEqualTo(42);
        assertThat(fetched.getType()).isEqualTo(OperationType.DEPOSIT);
        assertThat(fetched.getCurrency()).isEqualTo(Currency.EUR);
    }

    @Test
    void findByAccountIdOrderByDateDesc_shouldReturnEmptyWhenNoTransactions() {
        CustomerEntity c = createCustomer("kc-empty");
        AccountEntity a = createAccount(c, "ACC-E");
        List<TransactionEntity> results = transactionRepository.findByAccountIdOrderByDateDesc(a.getId());
        assertThat(results).isEmpty();
    }

    @Test
    void findByAccountIdOrderByDateDesc_shouldReturnOnlyForSpecifiedAccount() {
        CustomerEntity c = createCustomer("kc-multi");
        AccountEntity a1 = createAccount(c, "ACC-1");
        AccountEntity a2 = createAccount(c, "ACC-2");

        TransactionEntity t1 = createTx(a1, 1, OperationType.DEPOSIT, LocalDateTime.now().minusDays(2), 1);
        TransactionEntity t2 = createTx(a2, 2, OperationType.DEPOSIT, LocalDateTime.now().minusDays(1), 2);
        TransactionEntity t3 = createTx(a1, 3, OperationType.DEPOSIT, LocalDateTime.now(), 4);

        List<TransactionEntity> forA1 = transactionRepository.findByAccountIdOrderByDateDesc(a1.getId());
        assertThat(forA1).extracting(TransactionEntity::getAccount).extracting(AccountEntity::getId)
                .containsOnly(a1.getId());
        assertThat(forA1).extracting(TransactionEntity::getId)
                .containsExactly(t3.getId(), t1.getId());

        List<TransactionEntity> forA2 = transactionRepository.findByAccountIdOrderByDateDesc(a2.getId());
        assertThat(forA2).extracting(TransactionEntity::getId)
                .containsExactly(t2.getId());
    }
}
