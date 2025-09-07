package org.itinov.bankApp.mapper;

import org.itinov.bankApp.domain.model.Account;
import org.itinov.bankApp.domain.model.Customer;
import org.itinov.bankApp.domain.model.Transaction;
import org.itinov.bankApp.domain.enums.Currency;
import org.itinov.bankApp.domain.enums.OperationType;
import org.itinov.bankApp.dto.AccountDTO;
import org.itinov.bankApp.dto.CustomerDTO;
import org.itinov.bankApp.dto.TransactionDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BankAPIMapperTest {

    // Use the generated implementation directly
    private final BankAPIMapper mapper = new BankAPIMapperImpl();

    @Test
    void shouldMapCustomerBasicFields() {
        Customer customer = Customer.builder().id(2L).name("John Doe").email("john@doe.tld").build();

        CustomerDTO dto = mapper.toDTO(customer);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(2L);
        assertThat(dto.name()).isEqualTo("John Doe");
        assertThat(dto.email()).isEqualTo("john@doe.tld");
    }

    @Test
    void shouldMapCustomerList() {
        List<Customer> customers = List.of(
            Customer.builder().id(1L).name("A").email("a@a").build(),
            Customer.builder().id(2L).name("B").email("b@b").build()
        );

        List<CustomerDTO> dtos = mapper.toCustomerDTOs(customers);
        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).id()).isEqualTo(1L);
        assertThat(dtos.get(1).name()).isEqualTo("B");
    }

    @Test
    void shouldMapAccountWithTransactions_andTransactionUsesShallowAccount() {
        Customer customer = Customer.builder().id(1L).name("Jane").email("j@e").build();
        Account account = Account.builder()
            .id(10L).number("ACC-10").currency(Currency.EUR).balance(100.0).customer(customer).transactions(new ArrayList<>())
            .build();

        Transaction tx = Transaction.builder()
            .id(100L)
            .date(LocalDateTime.now())
            .amount(42.0)
            .type(OperationType.DEPOSIT)
            .currency(Currency.EUR)
            .performedBy("Jane")
            .balanceAfter(142.0)
            .account(account)
            .build();
        account.transactions().add(tx);

        AccountDTO adto = mapper.toDTO(account);
        assertThat(adto.id()).isEqualTo(10L);
        assertThat(adto.number()).isEqualTo("ACC-10");
        assertThat(adto.currency()).isEqualTo(Currency.EUR);
        assertThat(adto.balance()).isEqualTo(100.0);
        assertThat(adto.transactions()).hasSize(1);

        TransactionDTO tdto = adto.transactions().getFirst();
        assertThat(tdto.id()).isEqualTo(100L);
        assertThat(tdto.amount()).isEqualTo(42.0);
        assertThat(tdto.type()).isEqualTo(OperationType.DEPOSIT);
        assertThat(tdto.currency()).isEqualTo(Currency.EUR);
        assertThat(tdto.performedBy()).isEqualTo("Jane");
        assertThat(tdto.balanceAfter()).isEqualTo(142.0);

        // The transaction must reference a shallow account (no transactions) to avoid cycles
        assertThat(tdto.account()).isNotNull();
        assertThat(tdto.account().transactions()).isNull();
        assertThat(tdto.account().id()).isEqualTo(10L);
        assertThat(tdto.account().number()).isEqualTo("ACC-10");
        assertThat(tdto.account().currency()).isEqualTo(Currency.EUR);
    }

    @Test
    void shouldMapAccountList() {
        Account a1 = Account.builder().id(1L).number("A1").balance(10).currency(Currency.EUR).transactions(new ArrayList<>()).build();
        Account a2 = Account.builder().id(2L).number("A2").balance(20).currency(Currency.USD).transactions(new ArrayList<>()).build();

        List<AccountDTO> dtos = mapper.toAccountDTOs(List.of(a1, a2));
        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).id()).isEqualTo(1L);
        assertThat(dtos.get(1).currency()).isEqualTo(Currency.USD);
    }

    @Test
    void shouldMapTransactionAndUseShallowAccount() {
        Account account = Account.builder().id(5L).number("ACC-5").currency(Currency.EUR).transactions(new ArrayList<>()).build();
        Transaction tx = Transaction.builder()
            .id(501L)
            .date(LocalDateTime.now())
            .amount(5.5)
            .type(OperationType.WITHDRAWAL)
            .currency(Currency.EUR)
            .performedBy("Robot")
            .balanceAfter(4.5)
            .account(account)
            .build();

        TransactionDTO dto = mapper.toDTO(tx);
        assertThat(dto.account()).isNotNull();
        assertThat(dto.account().id()).isEqualTo(5L);
        // shallow mapping must ignore transactions
        assertThat(dto.account().transactions()).isNull();
    }

    @Test
    void shouldMapTransactionList() {
        Account account = Account.builder().id(7L).number("A7").currency(Currency.EUR).transactions(new ArrayList<>()).build();
        Transaction tx1 = Transaction.builder().id(1L).date(LocalDateTime.now()).amount(1).type(OperationType.DEPOSIT).currency(Currency.EUR).performedBy("P1").balanceAfter(1).account(account).build();
        Transaction tx2 = Transaction.builder().id(2L).date(LocalDateTime.now()).amount(2).type(OperationType.WITHDRAWAL).currency(Currency.EUR).performedBy("P2").balanceAfter(-1).account(account).build();

        List<TransactionDTO> list = mapper.toTransactionDTOs(List.of(tx1, tx2));
        assertThat(list).hasSize(2);
        assertThat(list.get(0).id()).isEqualTo(1L);
        assertThat(list.get(1).type()).isEqualTo(OperationType.WITHDRAWAL);
        assertThat(list.get(0).account().transactions()).isNull(); // still shallow
    }
}
