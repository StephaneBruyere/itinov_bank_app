package org.itinov.bankApp.service;

import org.itinov.bankApp.domain.model.Customer;
import org.itinov.bankApp.infrastructure.entity.CustomerEntity;
import org.itinov.bankApp.infrastructure.repository.CustomerRepository;
import org.itinov.bankApp.mapper.BankPersistenceMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CustomerServiceImplTest {

    private CustomerRepository customerRepository;
    private BankPersistenceMapper mapper;
    private CustomerServiceImpl service;

    @BeforeEach
    void setup() {
        customerRepository = mock(CustomerRepository.class);
        mapper = mock(BankPersistenceMapper.class);
        service = new CustomerServiceImpl(customerRepository, mapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findAllCustomers_shouldMapEntities() {
        when(customerRepository.findAll()).thenReturn(List.of(new CustomerEntity(), new CustomerEntity()));
        when(mapper.toDomain(any(CustomerEntity.class))).thenReturn(new Customer(1L, "111-111-111-111", "n", "e"));

        List<Customer> list = service.findAllCustomers();
        assertThat(list).hasSize(2);
    }

    @Test
    void getById_shouldReturn_whenPresent() {
        CustomerEntity c = new CustomerEntity();
        when(customerRepository.findById(5L)).thenReturn(Optional.of(c));
        when(mapper.toDomain(c)).thenReturn(new Customer(5L, "111-111-111-111", "n", "e"));

        Customer dto = service.getById(5L);
        assertThat(dto.id()).isEqualTo(5L);
    }

    @Test
    void getById_shouldThrow_whenMissing() {
        when(customerRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.getById(9L));
    }

    @Test
    void getCurrentCustomer_shouldExtractKeycloakSubFromJwt() {
        String sub = "abc-123";
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("sub", sub).build();
        Authentication auth = new JwtAuthenticationToken(jwt);
        SecurityContext context = Mockito.mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        CustomerEntity customer = CustomerEntity.builder().keycloakId(sub).build();
        when(customerRepository.findByKeycloakId(sub)).thenReturn(Optional.of(customer));
        when(mapper.toDomain(customer)).thenReturn(new Customer(1L, "111-111-111-111", "n", "e"));

        Customer current = service.getCurrentCustomer();
        assertThat(current).isNotNull();
        verify(customerRepository).findByKeycloakId(sub);
    }

    @Test
    void getCurrentCustomer_shouldThrow_whenNotFound() {
        String sub = "not-found";
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("sub", sub).build();
        Authentication auth = new JwtAuthenticationToken(jwt);
        SecurityContext context = Mockito.mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        when(customerRepository.findByKeycloakId(sub)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getCurrentCustomer());
    }
}
