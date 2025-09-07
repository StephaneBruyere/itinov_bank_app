package org.itinov.bankApp.mapper;

import org.itinov.bankApp.domain.model.Account;
import org.itinov.bankApp.domain.model.Customer;
import org.itinov.bankApp.domain.model.Transaction;
import org.itinov.bankApp.domain.enums.Currency;
import org.itinov.bankApp.domain.enums.OperationType;
import org.itinov.bankApp.infrastructure.entity.AccountEntity;
import org.itinov.bankApp.infrastructure.entity.CustomerEntity;
import org.itinov.bankApp.infrastructure.entity.TransactionEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BankPersistenceMapperTest {

    // Use the generated implementation directly
    private final BankPersistenceMapper mapper = new BankPersistenceMapperImpl();

    @Test
    void shouldMapCustomerBasicFields() {
        CustomerEntity customer = CustomerEntity.builder().id(2L).name("John Doe").email("john@doe.tld").build();

        Customer domain = mapper.toDomain(customer);

        assertThat(domain).isNotNull();
        assertThat(domain.id()).isEqualTo(2L);
        assertThat(domain.name()).isEqualTo("John Doe");
        assertThat(domain.email()).isEqualTo("john@doe.tld");
    }

    @Test
    void shouldMapCustomerList() {
        List<CustomerEntity> customers = List.of(
            CustomerEntity.builder().id(1L).name("A").email("a@a").build(),
            CustomerEntity.builder().id(2L).name("B").email("b@b").build()
        );

        List<Customer> domains = mapper.toDomainCustomers(customers);
        assertThat(domains).hasSize(2);
        assertThat(domains.get(0).id()).isEqualTo(1L);
        assertThat(domains.get(1).name()).isEqualTo("B");
    }

    @Test
    void shouldMapAccountWithTransactions_andTransactionUsesShallowAccount() {
        CustomerEntity customer = CustomerEntity.builder().id(1L).name("Jane").email("j@e").build();
        AccountEntity account = AccountEntity.builder()
            .id(10L).number("ACC-10").currency(Currency.EUR).balance(100.0).customer(customer).transactions(new ArrayList<>()).build();

        TransactionEntity tx = TransactionEntity.builder()
            .id(100L)
            .date(LocalDateTime.now())
            .amount(42.0)
            .type(OperationType.DEPOSIT)
            .currency(Currency.EUR)
            .performedBy("Jane")
            .balanceAfter(142.0)
            .account(account)
            .build();
        account.getTransactions().add(tx);

        Account acct = mapper.toDomain(account);
        assertThat(acct.id()).isEqualTo(10L);
        assertThat(acct.number()).isEqualTo("ACC-10");
        assertThat(acct.currency()).isEqualTo(Currency.EUR);
        assertThat(acct.balance()).isEqualTo(100.0);
        assertThat(acct.transactions()).hasSize(1);

        Transaction transac = acct.transactions().getFirst();
        assertThat(transac.id()).isEqualTo(100L);
        assertThat(transac.amount()).isEqualTo(42.0);
        assertThat(transac.type()).isEqualTo(OperationType.DEPOSIT);
        assertThat(transac.currency()).isEqualTo(Currency.EUR);
        assertThat(transac.performedBy()).isEqualTo("Jane");
        assertThat(transac.balanceAfter()).isEqualTo(142.0);

        // The transaction must reference a shallow account (no transactions) to avoid cycles
        assertThat(transac.account()).isNotNull();
        assertThat(transac.account().transactions()).isNull();
        assertThat(transac.account().id()).isEqualTo(10L);
        assertThat(transac.account().number()).isEqualTo("ACC-10");
        assertThat(transac.account().currency()).isEqualTo(Currency.EUR);
    }

    @Test
    void shouldMapAccountList() {
        AccountEntity a1 = AccountEntity.builder().id(1L).number("A1").balance(10).currency(Currency.EUR).transactions(new ArrayList<>()).build();
        AccountEntity a2 = AccountEntity.builder().id(2L).number("A2").balance(20).currency(Currency.USD).transactions(new ArrayList<>()).build();

        List<Account> domains = mapper.toDomainAccounts(List.of(a1, a2));
        assertThat(domains).hasSize(2);
        assertThat(domains.get(0).id()).isEqualTo(1L);
        assertThat(domains.get(1).currency()).isEqualTo(Currency.USD);
    }

    @Test
    void shouldMapTransactionAndUseShallowAccount() {
        AccountEntity account = AccountEntity.builder().id(5L).number("ACC-5").currency(Currency.EUR).transactions(new ArrayList<>()).build();
        TransactionEntity tx = TransactionEntity.builder()
            .id(501L)
            .date(LocalDateTime.now())
            .amount(5.5)
            .type(OperationType.WITHDRAWAL)
            .currency(Currency.EUR)
            .performedBy("Robot")
            .balanceAfter(4.5)
            .account(account)
            .build();

        Transaction domain = mapper.toDomain(tx);
        assertThat(domain.account()).isNotNull();
        assertThat(domain.account().id()).isEqualTo(5L);
        // shallow mapping must ignore transactions
        assertThat(domain.account().transactions()).isNull();
    }

    @Test
    void shouldMapTransactionList() {
        AccountEntity account = AccountEntity.builder().id(7L).number("A7").currency(Currency.EUR).transactions(new ArrayList<>()).build();
        TransactionEntity tx1 = TransactionEntity.builder().id(1L).date(LocalDateTime.now()).amount(1).type(OperationType.DEPOSIT).currency(Currency.EUR).performedBy("P1").balanceAfter(1).account(account).build();
        TransactionEntity tx2 = TransactionEntity.builder().id(2L).date(LocalDateTime.now()).amount(2).type(OperationType.WITHDRAWAL).currency(Currency.EUR).performedBy("P2").balanceAfter(-1).account(account).build();

        List<Transaction> list = mapper.toDomainTransactions(List.of(tx1, tx2));
        assertThat(list).hasSize(2);
        assertThat(list.get(0).id()).isEqualTo(1L);
        assertThat(list.get(1).type()).isEqualTo(OperationType.WITHDRAWAL);
        assertThat(list.get(0).account().transactions()).isNull(); // still shallow
    }
}
