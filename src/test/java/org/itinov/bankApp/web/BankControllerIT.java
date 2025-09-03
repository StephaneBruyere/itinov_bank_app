package org.itinov.bankApp.web;

import org.hamcrest.Matchers;
import org.itinov.bankApp.config.JwtTestConfig;
import org.itinov.bankApp.domain.model.enums.Currency;
import org.itinov.bankApp.dto.AccountDTO;
import org.itinov.bankApp.dto.CustomerDTO;
import org.itinov.bankApp.dto.TransactionDTO;
import org.itinov.bankApp.service.BankService;
import org.itinov.bankApp.service.CustomerService;
import org.itinov.bankApp.domain.model.enums.OperationType;
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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(JwtTestConfig.class)
@SpringBootTest
@AutoConfigureMockMvc
class BankControllerIT {

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    CustomerService customerService;
    @MockitoBean
    BankService bankService;

    private static String json(String s) {
        return s;
    }

    @Test
    @DisplayName("/api/accounts/{id}/transactions returns 401 when not authenticated")
    void getTransactionsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/accounts/{id}/transactions", 1)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/api/accounts/customer/{id} forbidden when authenticated user is not the customer")
    void getAccountsForbiddenForOtherCustomer() throws Exception {
        CustomerDTO fakeCustomer = new CustomerDTO(1L, "Jane Smith", "jane@example.com", List.of());
        Mockito.when(customerService.getCurrentCustomer()).thenReturn(fakeCustomer);
        mockMvc.perform(get("/api/accounts/customer/{id}", 999999)
                .header("Authorization", "Bearer fake-token")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
        Mockito.verify(customerService, Mockito.times(1)).getCurrentCustomer();
    }

    @Test
    @DisplayName("Deposit endpoint requires auth and enforces ownership (403 if not owner)")
    void depositForbiddenIfNotOwner() throws Exception {
        CustomerDTO fakeCustomer = new CustomerDTO(1L, "Jane Smith", "jane@example.com", List.of());
        Mockito.when(customerService.getCurrentCustomer()).thenReturn(fakeCustomer);
        mockMvc.perform(post("/api/accounts/{id}/deposit", 12345)
                .header("Authorization", "Bearer fake-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":10}")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
        Mockito.verify(customerService, Mockito.times(1)).getCurrentCustomer();
    }

    @Test
    @DisplayName("/api/accounts/{id}/transactions returns 400 when account not found and ownership passes")
    void getTransactionsBadRequestWhenAccountMissing() throws Exception {
        // Make current customer own account 999999 so the controller does not return 403
        CustomerDTO owner = new CustomerDTO(
            1L, "Jane Smith", "jane@example.com",
            List.of(new AccountDTO(999999L, "ACC-999", 0.0, Currency.EUR, List.of()))
        );
        Mockito.when(customerService.getCurrentCustomer()).thenReturn(owner);
        // Let service throw as if the repository did not find the account
        Mockito.when(bankService.getTransactionsByAccount(999999L))
            .thenThrow(new IllegalArgumentException("Account not found"));

        mockMvc.perform(get("/api/accounts/{accountId}/transactions", 999999L)
                .header("Authorization", "Bearer fake-token")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(Matchers.containsString("Account not found")));
    }

    @Test
    @DisplayName("Withdraw returns 400 when service throws overdraft exception and ownership passes")
    void withdrawOverdraftBadRequest() throws Exception {
        long accountId = 123L;
        CustomerDTO owner = new CustomerDTO(
            1L, "Jane Smith", "jane@example.com",
            List.of(new AccountDTO(accountId, "ACC-123", 0.0, Currency.EUR, List.of()))
        );
        Mockito.when(customerService.getCurrentCustomer()).thenReturn(owner);
        Mockito.when(bankService.withdraw(accountId, 5000.0, "jane"))
            .thenThrow(new IllegalArgumentException("Withdrawal would exceed overdraft limit"));

        mockMvc.perform(post("/api/accounts/{id}/withdraw", accountId)
                .header("Authorization", "Bearer fake-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":5000}")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(Matchers.containsString("overdraft")));
    }

    @Test
    @DisplayName("Transfer enforces ownership (403 if from account not owned)")
    void transferForbiddenIfNotOwner() throws Exception {
        // Current customer has no accounts
        CustomerDTO fakeCustomer = new CustomerDTO(1L, "Jane Smith", "jane@example.com", List.of());
        Mockito.when(customerService.getCurrentCustomer()).thenReturn(fakeCustomer);

        mockMvc.perform(post("/api/accounts/{id}/transfer", 111)
                .header("Authorization", "Bearer fake-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"toAccountId\":222,\"amount\":50}")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("/api/accounts/customer/{id} returns 200 and accounts for current customer")
    void getAccountsSuccess() throws Exception {
        long customerId = 1L;
        List<AccountDTO> accounts = List.of(
            new AccountDTO(10L, "ACC-10", 100.0, Currency.EUR, List.of()),
            new AccountDTO(11L, "ACC-11", 250.0, Currency.EUR, List.of())
        );
        CustomerDTO current = new CustomerDTO(customerId, "Jane Smith", "jane@example.com", accounts);
        Mockito.when(customerService.getCurrentCustomer()).thenReturn(current);
        Mockito.when(bankService.getAccountsByCustomer(customerId)).thenReturn(accounts);

        mockMvc.perform(get("/api/accounts/customer/{id}", customerId)
                .header("Authorization", "Bearer fake-token")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(10))
            .andExpect(jsonPath("$[1].id").value(11));
    }

    @Test
    @DisplayName("/api/accounts/{id}/transactions returns 200 for owned account")
    void getTransactionsSuccess() throws Exception {
        long accountId = 10L;
        CustomerDTO owner = new CustomerDTO(1L, "Jane Smith", "jane@example.com",
            List.of(new AccountDTO(accountId, "ACC-10", 100.0, Currency.EUR, List.of())));
        Mockito.when(customerService.getCurrentCustomer()).thenReturn(owner);
        Mockito.when(bankService.getTransactionsByAccount(accountId)).thenReturn(List.of());

        mockMvc.perform(get("/api/accounts/{id}/transactions", accountId)
                .header("Authorization", "Bearer fake-token")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("Deposit returns 201 and transaction for owned account")
    void depositSuccess() throws Exception {
        long accountId = 10L;
        CustomerDTO owner = new CustomerDTO(1L, "Jane Smith", "jane@example.com",
            List.of(new AccountDTO(accountId, "ACC-10", 100.0, Currency.EUR, List.of())));
        Mockito.when(customerService.getCurrentCustomer()).thenReturn(owner);
        // Build a minimal TransactionDTO via interface contract: id, date, amount, type, balance, description, accountId
        var tx = new TransactionDTO(
            1L,
            java.time.LocalDateTime.now(),
            50.0,
            OperationType.DEPOSIT,
            Currency.EUR,
            "jane",
            150.0,
            new AccountDTO(accountId, "ACC-10", 150.0, Currency.EUR, List.of())
        );
        Mockito.when(bankService.deposit(accountId, 50.0, "jane")).thenReturn(tx);

        mockMvc.perform(post("/api/accounts/{id}/deposit", accountId)
                .header("Authorization", "Bearer fake-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("{\"amount\":50}"))
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.amount").value(50.0))
            .andExpect(jsonPath("$.account.id").value((int) accountId));
    }

    @Test
    @DisplayName("Withdraw returns 201 and transaction for owned account")
    void withdrawSuccess() throws Exception {
        long accountId = 10L;
        CustomerDTO owner = new CustomerDTO(1L, "Jane Smith", "jane@example.com",
            List.of(new AccountDTO(accountId, "ACC-10", 100.0, Currency.EUR, List.of())));
        Mockito.when(customerService.getCurrentCustomer()).thenReturn(owner);
        var tx = new TransactionDTO(
            2L,
            java.time.LocalDateTime.now(),
            40.0,
            OperationType.WITHDRAWAL,
            Currency.EUR,
            "jane",
            60.0,
            new AccountDTO(accountId, "ACC-10", 60.0, Currency.EUR, List.of())
        );
        Mockito.when(bankService.withdraw(accountId, 40.0, "jane")).thenReturn(tx);

        mockMvc.perform(post("/api/accounts/{id}/withdraw", accountId)
                .header("Authorization", "Bearer fake-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("{\"amount\":40}"))
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.amount").value(40.0))
            .andExpect(jsonPath("$.account.id").value((int) accountId));
    }

    @Test
    @DisplayName("Transfer returns 201 and list of transactions for owned source account")
    void transferSuccess() throws Exception {
        long fromId = 10L;
        long toId = 11L;
        CustomerDTO owner = new CustomerDTO(1L, "Jane Smith", "jane@example.com",
            List.of(new AccountDTO(fromId, "ACC-10", 100.0, Currency.EUR, List.of())));
        Mockito.when(customerService.getCurrentCustomer()).thenReturn(owner);
        var debit = new TransactionDTO(
            3L,
            java.time.LocalDateTime.now(),
            30.0,
            OperationType.TRANSFER,
            Currency.EUR,
            "jane",
            70.0,
            new AccountDTO(fromId, "ACC-10", 70.0, Currency.EUR, List.of())
        );
        var credit = new TransactionDTO(
            4L,
            java.time.LocalDateTime.now(),
            30.0,
            OperationType.TRANSFER,
            Currency.EUR,
            "jane",
            130.0,
            new AccountDTO(toId, "ACC-11", 130.0, Currency.EUR, List.of())
        );
        Mockito.when(bankService.transfer(fromId, toId, 30.0, "jane"))
            .thenReturn(List.of(debit, credit));

        mockMvc.perform(post("/api/accounts/{id}/transfer", fromId)
                .header("Authorization", "Bearer fake-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("{\"toAccountId\":%d,\"amount\":30}".formatted(toId)))
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$[0].account.id").value((int) fromId))
            .andExpect(jsonPath("$[1].account.id").value((int) toId));
    }

}
