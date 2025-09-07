package org.itinov.bankApp.repository;

import org.itinov.bankApp.infrastructure.entity.AccountEntity;
import org.itinov.bankApp.infrastructure.entity.CustomerEntity;
import org.itinov.bankApp.domain.enums.Currency;
import org.itinov.bankApp.infrastructure.repository.AccountRepository;
import org.itinov.bankApp.infrastructure.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private CustomerEntity createCustomer(String keycloakId) {
        CustomerEntity c = CustomerEntity.builder().name("John Doe").email("john@example.com").keycloakId(keycloakId).build();
        return customerRepository.save(c);
    }

    private AccountEntity createAccount(CustomerEntity customer, String number, double balance) {
        AccountEntity a = AccountEntity.builder()
                .number(number)
                .balance(balance)
                .overdraftLimit(100)
                .currency(Currency.EUR)
                .customer(customer)
                .build();
        return accountRepository.save(a);
    }

    @Test
    void findByCustomerId_shouldReturnAccountsForCustomer() {
        CustomerEntity c1 = createCustomer("kc-1");
        CustomerEntity c2 = createCustomer("kc-2");
        AccountEntity a1 = createAccount(c1, "ACC-1", 50);
        AccountEntity a2 = createAccount(c1, "ACC-2", 75);
        AccountEntity a3 = createAccount(c2, "ACC-3", 20);

        List<AccountEntity> forC1 = accountRepository.findByCustomerId(c1.getId());
        List<AccountEntity> forC2 = accountRepository.findByCustomerId(c2.getId());

        assertThat(forC1).extracting(AccountEntity::getId).containsExactlyInAnyOrder(a1.getId(), a2.getId());
        assertThat(forC2).extracting(AccountEntity::getId).containsExactly(a3.getId());
    }

    @Test
    void existsByIdAndCustomerId_shouldReturnTrueForOwnedAccount_andFalseOtherwise() {
        CustomerEntity c1 = createCustomer("kc-1");
        CustomerEntity c2 = createCustomer("kc-2");
        AccountEntity a1 = createAccount(c1, "ACC-1", 50);

        boolean owned = accountRepository.existsByIdAndCustomerId(a1.getId(), c1.getId());
        boolean notOwned = accountRepository.existsByIdAndCustomerId(a1.getId(), c2.getId());
        boolean unknown = accountRepository.existsByIdAndCustomerId(9999L, c1.getId());

        assertThat(owned).isTrue();
        assertThat(notOwned).isFalse();
        assertThat(unknown).isFalse();
    }

    @Test
    void saveAndFind_shouldPersistAndRetrieveAccount() {
        CustomerEntity c1 = createCustomer("kc-x");
        AccountEntity created = createAccount(c1, "ACC-X", 123.45);

        Optional<AccountEntity> found = accountRepository.findById(created.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getNumber()).isEqualTo("ACC-X");
        assertThat(found.get().getBalance()).isEqualTo(123.45);
        assertThat(found.get().getCustomer().getId()).isEqualTo(c1.getId());
    }
}
