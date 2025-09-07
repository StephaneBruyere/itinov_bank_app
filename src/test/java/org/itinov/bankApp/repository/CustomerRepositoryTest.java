package org.itinov.bankApp.repository;

import org.itinov.bankApp.infrastructure.entity.CustomerEntity;
import org.itinov.bankApp.infrastructure.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void findByKeycloakId_shouldReturnCustomer() {
        CustomerEntity c = CustomerEntity.builder().name("n").email("e").keycloakId("kid").build();
        customerRepository.save(c);

        Optional<CustomerEntity> found = customerRepository.findByKeycloakId("kid");
        assertThat(found).isPresent();
        assertThat(found.get().getKeycloakId()).isEqualTo("kid");
    }

    @Test
    void findByKeycloakId_shouldReturnEmptyWhenNotFound() {
        Optional<CustomerEntity> notFound = customerRepository.findByKeycloakId("unknown-id");
        assertThat(notFound).isNotPresent();
    }
}
