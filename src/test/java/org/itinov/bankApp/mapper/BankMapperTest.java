package org.itinov.bankApp.mapper;

import org.itinov.bankApp.domain.model.Account;
import org.itinov.bankApp.domain.model.Customer;
import org.itinov.bankApp.domain.model.Transaction;
import org.itinov.bankApp.domain.model.enums.Currency;
import org.itinov.bankApp.domain.model.enums.OperationType;
import org.itinov.bankApp.dto.AccountDTO;
import org.itinov.bankApp.dto.CustomerDTO;
import org.itinov.bankApp.dto.TransactionDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class BankMapperTest {

    // Use the generated implementation directly
    private final BankMapper mapper = new BankMapperImpl();

    @Test
    void shouldMapCustomerWithAccountsAndTransactions() {
        Customer customer = Customer.builder().id(1L).name("Jane").email("j@e").build();
        Account account = Account.builder().id(10L).number("ACC-10").currency(Currency.EUR).customer(customer).build();
        customer.getAccounts().add(account);

        Transaction tx = Transaction.builder()
            .id(100L)
            .date(LocalDateTime.now())
            .amount(42.0)
            .type(OperationType.DEPOSIT)
            .currency(Currency.EUR)
            .performedBy("Jane")
            .balanceAfter(100.0)
            .account(account)
            .build();
        account.getTransactions().add(tx);

        CustomerDTO dto = mapper.toDTO(customer);
        assertThat(dto.accounts()).hasSize(1);
        AccountDTO adto = dto.accounts().getFirst();
        assertThat(adto.transactions()).hasSize(1);
        TransactionDTO tdto = adto.transactions().getFirst();

        // The transaction must reference a shallow account (no transactions) to avoid cycles
        assertThat(tdto.account()).isNotNull();
        assertThat(tdto.account().transactions()).isNull();
        assertThat(tdto.account().id()).isEqualTo(10L);
    }
}
