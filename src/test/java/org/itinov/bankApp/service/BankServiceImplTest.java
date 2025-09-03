package org.itinov.bankApp.service;

import org.itinov.bankApp.domain.model.Account;
import org.itinov.bankApp.domain.model.Transaction;
import org.itinov.bankApp.domain.model.enums.Currency;
import org.itinov.bankApp.domain.model.enums.OperationType;
import org.itinov.bankApp.domain.repository.AccountRepository;
import org.itinov.bankApp.domain.repository.TransactionRepository;
import org.itinov.bankApp.dto.AccountDTO;
import org.itinov.bankApp.dto.CustomerDTO;
import org.itinov.bankApp.dto.TransactionDTO;
import org.itinov.bankApp.mapper.BankMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class BankServiceImplTest {

    private CustomerService customerService;
    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private BankMapper mapper;
    private BankServiceImpl service;

    @BeforeEach
    void setup() {
        customerService = mock(CustomerService.class);
        accountRepository = mock(AccountRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        mapper = mock(BankMapper.class);
        service = new BankServiceImpl(customerService, accountRepository, transactionRepository, mapper);
    }

    private CustomerDTO customerWithAccounts(Long... ids) {
        List<AccountDTO> accounts = java.util.Arrays.stream(ids)
                .map(id -> new AccountDTO(id, "ACC-" + id, 0.0, Currency.EUR, List.of()))
                .toList();
        return new CustomerDTO(99L, "Test", "t@test", accounts);
    }

    @Test
    void getTransactionsByAccount_shouldMapToDTOs() {
        Account account = Account.builder().id(1L).currency(Currency.EUR).build();
        account.setTransactions(List.of(new Transaction(), new Transaction()));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(mapper.toDTO(any(Transaction.class))).thenReturn(new TransactionDTO(1L, null, 0, OperationType.DEPOSIT, Currency.EUR, null, 0, null));

        List<TransactionDTO> list = service.getTransactionsByAccount(1L);
        assertThat(list).hasSize(2);
    }

    @Test
    void getTransactionsByAccount_shouldThrow_whenAccountMissing() {
        when(accountRepository.findById(42L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.getTransactionsByAccount(42L));
    }

    @Test
    void getAccountsByCustomer_shouldReturnFromCustomerService() {
        CustomerDTO customer = new CustomerDTO(10L, "n", "e", List.of());
        when(customerService.getById(10L)).thenReturn(customer);

        List<AccountDTO> result = service.getAccountsByCustomer(10L);
        assertThat(result).isSameAs(customer.accounts());
    }

    @Test
    void deposit_shouldIncreaseBalanceAndPersistTransaction() {
        Long accId = 1L;
        Account account = Account.builder().id(accId).number("ACC-1").balance(100.0).currency(Currency.EUR).overdraftLimit(-200).build();

        when(customerService.getCurrentCustomer()).thenReturn(customerWithAccounts(accId));
        when(accountRepository.findById(accId)).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toDTO(any(Transaction.class))).thenReturn(new TransactionDTO(10L, null, 50.0, OperationType.DEPOSIT, Currency.EUR, "me", 150.0, null));

        TransactionDTO dto = service.deposit(accId, 50.0, "me");

        assertThat(dto.type()).isEqualTo(OperationType.DEPOSIT);
        assertThat(account.getBalance()).isEqualTo(150.0);

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction saved = txCaptor.getValue();
        assertThat(saved.getType()).isEqualTo(OperationType.DEPOSIT);
        assertThat(saved.getAmount()).isEqualTo(50.0);
        assertThat(saved.getBalanceAfter()).isEqualTo(150.0);
        assertThat(saved.getAccount()).isEqualTo(account);

        verify(accountRepository).save(account);
    }

    @Test
    void deposit_shouldFailIfAccountNotOwnedByCurrentCustomer() {
        Long accId = 1L;
        Account account = Account.builder().id(accId).balance(100.0).currency(Currency.EUR).build();
        when(customerService.getCurrentCustomer()).thenReturn(customerWithAccounts(2L));
        when(accountRepository.findById(accId)).thenReturn(Optional.of(account));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.deposit(accId, 10.0, "me"));
        assertThat(ex.getMessage()).contains("does not belong");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void deposit_shouldFail_whenAccountNotFound() {
        Long accId = 1L;
        when(accountRepository.findById(accId)).thenReturn(Optional.empty());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.deposit(accId, 10.0, "me"));
        assertThat(ex.getMessage()).contains("Account not found");
        verify(transactionRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void withdraw_shouldDecreaseBalance_whenWithinOverdraft() {
        Long accId = 1L;
        Account account = Account.builder().id(accId).number("ACC-1").balance(100.0).currency(Currency.EUR).overdraftLimit(-200).build();
        when(customerService.getCurrentCustomer()).thenReturn(customerWithAccounts(accId));
        when(accountRepository.findById(accId)).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toDTO(any(Transaction.class))).thenReturn(new TransactionDTO(10L, null, 50.0, OperationType.WITHDRAWAL, Currency.EUR, "me", 50.0, null));

        TransactionDTO dto = service.withdraw(accId, 50.0, "me");

        assertThat(dto.type()).isEqualTo(OperationType.WITHDRAWAL);
        assertThat(account.getBalance()).isEqualTo(50.0);

        verify(accountRepository).save(account);
    }

    @Test
    void withdraw_shouldFail_whenOverdraftExceeded() {
        Long accId = 1L;
        Account account = Account.builder().id(accId).balance(-150.0).overdraftLimit(-200.0).currency(Currency.EUR).build();
        when(customerService.getCurrentCustomer()).thenReturn(customerWithAccounts(accId));
        when(accountRepository.findById(accId)).thenReturn(Optional.of(account));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.withdraw(accId, 60.0, "me"));
        assertThat(ex.getMessage()).contains("overdraft");
        verify(transactionRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void withdraw_shouldFail_whenAccountNotFound() {
        Long accId = 1L;
        when(accountRepository.findById(accId)).thenReturn(Optional.empty());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.withdraw(accId, 10.0, "me"));
        assertThat(ex.getMessage()).contains("Account not found");
        verify(transactionRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void withdraw_shouldFail_ifAccountNotOwned() {
        Long accId = 1L;
        Account account = Account.builder().id(accId).balance(100.0).currency(Currency.EUR).overdraftLimit(-200).build();
        when(customerService.getCurrentCustomer()).thenReturn(customerWithAccounts(2L));
        when(accountRepository.findById(accId)).thenReturn(Optional.of(account));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.withdraw(accId, 10.0, "me"));
        assertThat(ex.getMessage()).contains("does not belong");
        verify(transactionRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void transfer_shouldMoveFundsAndCreateTwoTransactions() {
        Long fromId = 1L, toId = 2L;
        Account from = Account.builder().id(fromId).balance(200.0).overdraftLimit(-200.0).currency(Currency.EUR).build();
        Account to = Account.builder().id(toId).balance(50.0).overdraftLimit(-200.0).currency(Currency.EUR).build();

        when(accountRepository.findById(fromId)).thenReturn(Optional.of(from));
        when(accountRepository.findById(toId)).thenReturn(Optional.of(to));
        when(customerService.getCurrentCustomer()).thenReturn(customerWithAccounts(fromId));
        when(mapper.toDTO(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            return new TransactionDTO(null, t.getDate(), t.getAmount(), t.getType(), t.getCurrency(), t.getPerformedBy(), t.getBalanceAfter(), null);
        });

        List<TransactionDTO> result = service.transfer(fromId, toId, 70.0, "me");

        assertThat(from.getBalance()).isEqualTo(130.0);
        assertThat(to.getBalance()).isEqualTo(120.0);
        assertThat(result).hasSize(2);
        verify(transactionRepository).saveAll(anyList());
        verify(accountRepository).saveAll(anyList());
    }

    @Test
    void transfer_shouldFail_forSameAccount() {
        Long fromId = 1L, toId = 1L;
        Account from = Account.builder().id(fromId).build();
        when(accountRepository.findById(fromId)).thenReturn(Optional.of(from));
        when(customerService.getCurrentCustomer()).thenReturn(customerWithAccounts(fromId));
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> service.transfer(fromId, toId, 10.0, "me"));
        assertThat(ex.getMessage()).contains("same account");
    }

    @Test
    void transfer_shouldFail_whenFromAccountNotFound() {
        Long fromId = 1L, toId = 2L;
        when(accountRepository.findById(fromId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.transfer(fromId, toId, 10.0, "me"));
        assertThat(ex.getMessage()).contains("From account not found");
        verify(transactionRepository, never()).saveAll(anyList());
        verify(accountRepository, never()).saveAll(anyList());
    }

    @Test
    void transfer_shouldFail_whenToAccountNotFound() {
        Long fromId = 1L, toId = 2L;
        Account from = Account.builder().id(fromId).balance(100.0).overdraftLimit(-200.0).currency(Currency.EUR).build();
        when(accountRepository.findById(fromId)).thenReturn(Optional.of(from));
        when(accountRepository.findById(toId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.transfer(fromId, toId, 10.0, "me"));
        assertThat(ex.getMessage()).contains("To account not found");
        verify(transactionRepository, never()).saveAll(anyList());
        verify(accountRepository, never()).saveAll(anyList());
    }

    @Test
    void transfer_shouldFail_whenFromAccountNotOwned() {
        Long fromId = 1L, toId = 2L;
        Account from = Account.builder().id(fromId).balance(100.0).overdraftLimit(-200.0).currency(Currency.EUR).build();
        Account to = Account.builder().id(toId).balance(50.0).overdraftLimit(-200.0).currency(Currency.EUR).build();
        when(customerService.getCurrentCustomer()).thenReturn(customerWithAccounts(99L));
        when(accountRepository.findById(fromId)).thenReturn(Optional.of(from));
        when(accountRepository.findById(toId)).thenReturn(Optional.of(to));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.transfer(fromId, toId, 10.0, "me"));
        assertThat(ex.getMessage()).contains("does not belong");
        verify(transactionRepository, never()).saveAll(anyList());
        verify(accountRepository, never()).saveAll(anyList());
    }

    @Test
    void transfer_shouldFail_whenOverdraftExceeded() {
        Long fromId = 1L, toId = 2L;
        Account from = Account.builder().id(fromId).balance(0.0).overdraftLimit(-100.0).currency(Currency.EUR).build();
        Account to = Account.builder().id(toId).balance(0.0).overdraftLimit(-100.0).currency(Currency.EUR).build();
        when(customerService.getCurrentCustomer()).thenReturn(customerWithAccounts(fromId));
        when(accountRepository.findById(fromId)).thenReturn(Optional.of(from));
        when(accountRepository.findById(toId)).thenReturn(Optional.of(to));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.transfer(fromId, toId, 150.0, "me"));
        assertThat(ex.getMessage()).contains("overdraft");
        verify(transactionRepository, never()).saveAll(anyList());
        verify(accountRepository, never()).saveAll(anyList());
    }

}
