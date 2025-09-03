package org.itinov.bankApp.service;

import org.itinov.bankApp.domain.model.Customer;
import org.itinov.bankApp.domain.repository.CustomerRepository;
import org.itinov.bankApp.dto.CustomerDTO;
import org.itinov.bankApp.mapper.BankMapper;
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
    private BankMapper mapper;
    private CustomerServiceImpl service;

    @BeforeEach
    void setup() {
        customerRepository = mock(CustomerRepository.class);
        mapper = mock(BankMapper.class);
        service = new CustomerServiceImpl(customerRepository, mapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findAllCustomers_shouldMapEntities() {
        when(customerRepository.findAll()).thenReturn(List.of(new Customer(), new Customer()));
        when(mapper.toDTO(any(Customer.class))).thenReturn(new CustomerDTO(1L, "n", "e", List.of()));

        List<CustomerDTO> list = service.findAllCustomers();
        assertThat(list).hasSize(2);
    }

    @Test
    void getById_shouldReturn_whenPresent() {
        Customer c = new Customer();
        when(customerRepository.findById(5L)).thenReturn(Optional.of(c));
        when(mapper.toDTO(c)).thenReturn(new CustomerDTO(5L, "n", "e", List.of()));

        CustomerDTO dto = service.getById(5L);
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

        Customer customer = Customer.builder().keycloakId(sub).build();
        when(customerRepository.findByKeycloakId(sub)).thenReturn(Optional.of(customer));
        when(mapper.toDTO(customer)).thenReturn(new CustomerDTO(1L, "n", "e", List.of()));

        CustomerDTO current = service.getCurrentCustomer();
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

        assertThrows(RuntimeException.class, () -> service.getCurrentCustomer());
    }
}
