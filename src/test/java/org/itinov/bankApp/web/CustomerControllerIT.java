package org.itinov.bankApp.web;

import org.itinov.bankApp.config.JwtTestConfig;
import org.itinov.bankApp.dto.CustomerDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(JwtTestConfig.class)
@SpringBootTest
@AutoConfigureMockMvc
class CustomerControllerIT {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    org.itinov.bankApp.service.CustomerService customerService;

    @Test
    @DisplayName("/api/public/customers is publicly accessible and returns 200")
    void publicCustomersAccessible() throws Exception {
        // Return an empty list to avoid NPE in the controller when using MockitoBean
        Mockito.when(customerService.findAllCustomers()).thenReturn(java.util.List.of());
        mockMvc.perform(get("/api/public/customers").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("/api/customer requires authenticated ROLE_customer and returns id")
    void currentCustomerRequiresAuth() throws Exception {
        // Stub current customer to simulate authenticated user
        Mockito.when(customerService.getCurrentCustomer())
            .thenReturn(new CustomerDTO(1L, "Jane Smith", "jane@example.com", java.util.List.of()));
        mockMvc.perform(get("/api/customer")
                .header("Authorization", "Bearer fake-token")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").value(1));
    }

    @Test
    @DisplayName("/api/customer returns 401 when not authenticated")
    void currentCustomerUnauthorized() throws Exception {
        mockMvc.perform(get("/api/customer").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/api/customer returns 400 when service throws (customer not found)")
    void currentCustomerNotFoundHandledByAdvice() throws Exception {
        Mockito.when(customerService.getCurrentCustomer()).thenThrow(new RuntimeException("Customer not found"));
        mockMvc.perform(get("/api/customer")
                .header("Authorization", "Bearer fake-token")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Customer not found")));
    }
}
