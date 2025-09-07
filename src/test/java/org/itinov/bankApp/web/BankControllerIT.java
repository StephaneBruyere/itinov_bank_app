package org.itinov.bankApp.web;

import org.hamcrest.Matchers;
import org.itinov.bankApp.config.JwtTestConfig;
import org.itinov.bankApp.domain.model.Account;
import org.itinov.bankApp.domain.model.Transaction;
import org.itinov.bankApp.domain.enums.Currency;
import org.itinov.bankApp.service.BankService;
import org.itinov.bankApp.service.CustomerService;
import org.itinov.bankApp.domain.enums.OperationType;
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
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
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
        Mockito.when(bankService.getAccountsByCustomer(999999L))
            .thenThrow(new AccessDeniedException("Forbidden"));
        mockMvc.perform(get("/api/accounts/customer/{id}", 999999)
                .header("Authorization", "Bearer fake-token")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deposit endpoint requires auth and enforces ownership (403 if not owner)")
    void depositForbiddenIfNotOwner() throws Exception {
        Mockito.when(bankService.deposit(12345L, 10.0, "jane"))
            .thenThrow(new AccessDeniedException("Forbidden"));
        mockMvc.perform(post("/api/accounts/{id}/deposit", 12345)
                .header("Authorization", "Bearer fake-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":10}")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("/api/accounts/{id}/transactions returns 400 when account not found and ownership passes")
    void getTransactionsBadRequestWhenAccountMissing() throws Exception {
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
        Mockito.when(bankService.transfer(111L, 222L, 50.0, "jane"))
            .thenThrow(new AccessDeniedException("Forbidden"));

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
        List<Account> accounts = List.of(
            Account.builder()
                .id(10L)
                .number("ACC-10")
                .balance(100.0)
                .overdraftLimit(150)
                .currency(Currency.EUR)
                .transactions(List.of())
                .build(),
            Account.builder()
                .id(11L)
                .number("ACC-11")
                .balance(2500.0)
                .overdraftLimit(100)
                .currency(Currency.EUR)
                .transactions(List.of())
                .build()
        );
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
        // Build a minimal TransactionDTO via interface contract: id, date, amount, type, balance, description, accountId
        var tx = Transaction.builder()
            .id(1L)
            .date(LocalDateTime.now())
            .amount(50.0)
            .type(OperationType.DEPOSIT)
            .currency(Currency.EUR)
            .performedBy("jane")
            .balanceAfter(150.0)
            .account(Account.builder()
                .id(accountId)
                .number("ACC-10")
                .balance(150.0)
                .currency(Currency.EUR)
                .transactions(List.of())
                .build()
            )
            .build();
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
        var tx = Transaction.builder()
            .id(2L)
            .date(LocalDateTime.now())
            .amount(40.0)
            .type(OperationType.WITHDRAWAL)
            .currency(Currency.EUR)
            .performedBy("jane")
            .balanceAfter(60.0)
            .account(Account.builder()
                .id(accountId)
                .number("ACC-10")
                .balance(60.0)
                .currency(Currency.EUR)
                .transactions(List.of())
                .build()
            ).build();
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
        var debit = Transaction.builder()
            .id(3L)
            .date(java.time.LocalDateTime.now())
            .amount(30.0)
            .type(OperationType.TRANSFER)
            .currency(Currency.EUR)
            .performedBy("jane")
            .balanceAfter(70.0)
            .account(Account.builder()
                .id(fromId)
                .number("ACC-10")
                .balance(70.0)
                .currency(Currency.EUR)
                .transactions(List.of())
                .build()
            ).build();
        var credit = Transaction.builder()
            .id(4L)
            .date(java.time.LocalDateTime.now())
            .amount(30.0)
            .type(OperationType.TRANSFER)
            .currency(Currency.EUR)
            .performedBy("jane")
            .balanceAfter(130.0)
            .account(Account.builder()
                .id(toId)
                .number("ACC-11")
                .balance(130.0)
                .currency(Currency.EUR)
                .transactions(List.of())
                .build()
            ).build();

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

        @Test
        @DisplayName("Deposit returns 404 when account not found")
        void depositNotFoundReturns404() throws Exception {
            long accountId = 9999L;
            Mockito.when(bankService.deposit(accountId, 10.0, "jane"))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("Account not found"));

            mockMvc.perform(post("/api/accounts/{id}/deposit", accountId)
                    .header("Authorization", "Bearer fake-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"amount\":10}")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Account not found")));
        }

        @Test
        @DisplayName("Withdraw returns 404 when account not found")
        void withdrawNotFoundReturns404() throws Exception {
            long accountId = 9999L;
            Mockito.when(bankService.withdraw(accountId, 10.0, "jane"))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("Account not found"));

            mockMvc.perform(post("/api/accounts/{id}/withdraw", accountId)
                    .header("Authorization", "Bearer fake-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"amount\":10}")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Account not found")));
        }

        @Test
        @DisplayName("Transfer returns 404 when destination account not found")
        void transferToAccountNotFoundReturns404() throws Exception {
            long fromId = 10L;
            long toId = 8888L;
            Mockito.when(bankService.transfer(fromId, toId, 10.0, "jane"))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("To account not found"));

            mockMvc.perform(post("/api/accounts/{id}/transfer", fromId)
                    .header("Authorization", "Bearer fake-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json("{\"toAccountId\":%d,\"amount\":10}".formatted(toId)))
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("To account not found")));
        }

}


