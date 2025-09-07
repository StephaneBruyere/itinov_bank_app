package org.itinov.bankApp.service;

import org.itinov.bankApp.domain.model.Account;
import org.itinov.bankApp.domain.model.Customer;
import org.itinov.bankApp.domain.model.Transaction;
import org.itinov.bankApp.infrastructure.entity.AccountEntity;
import org.itinov.bankApp.infrastructure.entity.CustomerEntity;
import org.itinov.bankApp.infrastructure.entity.TransactionEntity;
import org.itinov.bankApp.domain.enums.Currency;
import org.itinov.bankApp.domain.enums.OperationType;
import org.itinov.bankApp.infrastructure.repository.AccountRepository;
import org.itinov.bankApp.infrastructure.repository.TransactionRepository;
import org.itinov.bankApp.mapper.BankPersistenceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import jakarta.persistence.EntityNotFoundException;

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
    private BankPersistenceMapper mapper;
    private BankServiceImpl service;

    @BeforeEach
    void setup() {
        customerService = mock(CustomerService.class);
        accountRepository = mock(AccountRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        mapper = mock(BankPersistenceMapper.class);
        service = new BankServiceImpl(customerService, accountRepository, transactionRepository, mapper);
    }

    @Test
    void getTransactionsByAccount_shouldMapToDTOs() {
        AccountEntity account = AccountEntity.builder().id(1L).currency(Currency.EUR).build();
        account.setTransactions(List.of(new TransactionEntity(), new TransactionEntity()));
        when(accountRepository.existsByIdAndCustomerId(1L, 99L)).thenReturn(true);
        when(transactionRepository.findByAccountIdOrderByDateDesc(1L)).thenReturn(List.of(new TransactionEntity(), new TransactionEntity()));
        when(customerService.getCurrentCustomer()).thenReturn(createCustomer());
        when(mapper.toDomain(any(TransactionEntity.class)))
            .thenReturn(
                new Transaction(1L, null, 0, OperationType.DEPOSIT, Currency.EUR, null, 0, null)
            );

        List<Transaction> list = service.getTransactionsByAccount(1L);
        assertThat(list).hasSize(2);
    }

    @Test
    void getTransactionsByAccount_shouldThrow_whenAccountMissing() {
        when(customerService.getCurrentCustomer()).thenReturn(createCustomer());
        when(accountRepository.existsByIdAndCustomerId(42L, 99L)).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> service.getTransactionsByAccount(42L));
    }

    @Test
    void getAccountsByCustomer_shouldReturnFromCustomerService() {
        when(customerService.getCurrentCustomer()).thenReturn(createCustomer());
        var acc1 = AccountEntity.builder().id(1L).number("ACC-1").build();
        var acc2 = AccountEntity.builder().id(2L).number("ACC-2").build();
        when(accountRepository.findByCustomerId(99L)).thenReturn(List.of(acc1, acc2));
        when(mapper.toShallow(acc1)).thenReturn(new Account(1L, "ACC-1", 0.0, 150, Currency.EUR, createCustomer(), List.of()));
        when(mapper.toShallow(acc2)).thenReturn(new Account(2L, "ACC-2", 0.0, 200, Currency.EUR, createCustomer(), List.of()));

        List<Account> result = service.getAccountsByCustomer(99L);
        assertThat(result).hasSize(2);
    }

    @Test
    void getAccountsByCustomer_shouldFail_whenRequestingAnotherCustomer() {
        // current logged customer is 99, but we request for 11
        when(customerService.getCurrentCustomer()).thenReturn(createCustomer());
        assertThrows(AccessDeniedException.class, () -> service.getAccountsByCustomer(11L));
        verify(accountRepository, never()).findByCustomerId(anyLong());
    }

    @Test
    void deposit_shouldFail_whenAmountNotPositive() {
        Long accId = 1L;
        AccountEntity account = AccountEntity.builder()
            .id(accId).balance(100.0).customer(createCustomerEntity()).currency(Currency.EUR).overdraftLimit(-200).build();
        when(customerService.getCurrentCustomer()).thenReturn(createCustomer());
        when(accountRepository.findById(accId)).thenReturn(Optional.of(account));

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> service.deposit(accId, 0.0, "me"));
        assertThat(ex1.getMessage()).contains("positive");
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> service.deposit(accId, -5.0, "me"));
        assertThat(ex2.getMessage()).contains("positive");
        verify(transactionRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void deposit_shouldIncreaseBalanceAndPersistTransaction() {
        Long accId = 1L;
        AccountEntity account = AccountEntity.builder()
            .id(accId).number("ACC-1").balance(100.0).customer(createCustomerEntity()).currency(Currency.EUR).overdraftLimit(-200).build();

        when(customerService.getCurrentCustomer()).thenReturn(createCustomer());
        when(accountRepository.findById(accId)).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toDomain(any(TransactionEntity.class)))
            .thenReturn(
                new Transaction(10L, null, 50.0, OperationType.DEPOSIT, Currency.EUR, "me", 150.0, null)
            );

        Transaction dto = service.deposit(accId, 50.0, "me");

        assertThat(dto.type()).isEqualTo(OperationType.DEPOSIT);
        assertThat(account.getBalance()).isEqualTo(150.0);

        ArgumentCaptor<TransactionEntity> txCaptor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionRepository).save(txCaptor.capture());
        TransactionEntity saved = txCaptor.getValue();
        assertThat(saved.getType()).isEqualTo(OperationType.DEPOSIT);
        assertThat(saved.getAmount()).isEqualTo(50.0);
        assertThat(saved.getBalanceAfter()).isEqualTo(150.0);
        assertThat(saved.getAccount()).isEqualTo(account);

        verify(accountRepository).save(account);
    }

    @Test
    void deposit_shouldFailIfAccountNotOwnedByCurrentCustomer() {
        Long accId = 1L;
        AccountEntity account = AccountEntity.builder()
            .id(accId).balance(100.0).customer(createOtherCustomerEntity()).currency(Currency.EUR).build();
        when(customerService.getCurrentCustomer()).thenReturn(createCustomer());
        when(accountRepository.findById(accId)).thenReturn(Optional.of(account));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> service.deposit(accId, 10.0, "me"));
        assertThat(ex.getMessage()).contains("does not belong");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void deposit_shouldFail_whenAccountNotFound() {
        Long accId = 1L;
        when(accountRepository.findById(accId)).thenReturn(Optional.empty());
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> service.deposit(accId, 10.0, "me"));
        assertThat(ex.getMessage()).contains("Account not found");
        verify(transactionRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void withdraw_shouldFail_whenAmountNotPositive() {
        Long accId = 1L;
        AccountEntity account = AccountEntity.builder()
            .id(accId).balance(100.0).customer(createCustomerEntity()).currency(Currency.EUR).overdraftLimit(-200).build();
        when(customerService.getCurrentCustomer()).thenReturn(createCustomer());
        when(accountRepository.findById(accId)).thenReturn(Optional.of(account));
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> service.withdraw(accId, 0.0, "me"));
        assertThat(ex1.getMessage()).contains("positive");
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> service.withdraw(accId, -1.0, "me"));
        assertThat(ex2.getMessage()).contains("positive");
        verify(transactionRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void withdraw_shouldDecreaseBalance_whenWithinOverdraft() {
        Long accId = 1L;
        AccountEntity account = AccountEntity.builder()
            .id(accId).number("ACC-1").balance(100.0).customer(createCustomerEntity()).currency(Currency.EUR).overdraftLimit(-200)
            .build();
        when(customerService.getCurrentCustomer()).thenReturn(createCustomer());
        when(accountRepository.findById(accId)).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toDomain(any(TransactionEntity.class)))
            .thenReturn(
                new Transaction(10L, null, 50.0, OperationType.WITHDRAWAL, Currency.EUR, "me", 50.0, null)
            );

        Transaction transaction = service.withdraw(accId, 50.0, "me");

        assertThat(transaction.type()).isEqualTo(OperationType.WITHDRAWAL);
        assertThat(account.getBalance()).isEqualTo(50.0);

        verify(accountRepository).save(account);
    }

    @Test
    void withdraw_shouldFail_whenOverdraftExceeded() {
        Long accId = 1L;
        AccountEntity account = AccountEntity.builder()
            .id(accId).balance(-150.0).overdraftLimit(-200.0).customer(createCustomerEntity()).currency(Currency.EUR).build();
        when(customerService.getCurrentCustomer()).thenReturn(createCustomer());
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
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> service.withdraw(accId, 10.0, "me"));
        assertThat(ex.getMessage()).contains("Account not found");
        verify(transactionRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void withdraw_shouldFail_ifAccountNotOwned() {
        Long accId = 1L;
        AccountEntity account = AccountEntity.builder()
            .id(accId).balance(100.0).customer(createOtherCustomerEntity()).currency(Currency.EUR).overdraftLimit(-200).build();
        when(customerService.getCurrentCustomer()).thenReturn(createCustomer());
        when(accountRepository.findById(accId)).thenReturn(Optional.of(account));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> service.withdraw(accId, 10.0, "me"));
        assertThat(ex.getMessage()).contains("does not belong");
        verify(transactionRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void transfer_shouldMoveFundsAndCreateTwoTransactions() {
        Long fromId = 1L, toId = 2L;
        AccountEntity from = AccountEntity.builder()
            .id(fromId).balance(200.0).overdraftLimit(-200.0).customer(createCustomerEntity()).currency(Currency.EUR).build();
        AccountEntity to = AccountEntity.builder()
            .id(toId).balance(50.0).customer(createOtherCustomerEntity()).currency(Currency.EUR).build();

        when(accountRepository.findById(fromId)).thenReturn(Optional.of(from));
        when(accountRepository.findById(toId)).thenReturn(Optional.of(to));
        when(customerService.getCurrentCustomer()).thenReturn(createCustomer());
        when(mapper.toDomain(any(TransactionEntity.class))).thenAnswer(inv -> {
            TransactionEntity t = inv.getArgument(0);
            return new Transaction(null, t.getDate(), t.getAmount(), t.getType(), t.getCurrency(), t.getPerformedBy(), t.getBalanceAfter(), null);
        });

        List<Transaction> result = service.transfer(fromId, toId, 70.0, "me");

        assertThat(from.getBalance()).isEqualTo(130.0);
        assertThat(to.getBalance()).isEqualTo(120.0);
        assertThat(result).hasSize(2);
        verify(transactionRepository).saveAll(anyList());
        verify(accountRepository).saveAll(anyList());
    }

    @Test
    void transfer_shouldFail_whenAmountNotPositive() {
        Long fromId = 1L, toId = 2L;
        AccountEntity from = AccountEntity.builder()
            .id(fromId).balance(100.0).overdraftLimit(-200.0).customer(createCustomerEntity()).currency(Currency.EUR).build();
        AccountEntity to = AccountEntity.builder().id(toId).balance(50.0).overdraftLimit(-200.0).currency(Currency.EUR).build();
        when(customerService.getCurrentCustomer()).thenReturn(createCustomer());
        when(accountRepository.findById(fromId)).thenReturn(Optional.of(from));
        when(accountRepository.findById(toId)).thenReturn(Optional.of(to));

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> service.transfer(fromId, toId, 0.0, "me"));
        assertThat(ex1.getMessage()).contains("positive");
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> service.transfer(fromId, toId, -10.0, "me"));
        assertThat(ex2.getMessage()).contains("positive");
        verify(transactionRepository, never()).saveAll(anyList());
        verify(accountRepository, never()).saveAll(anyList());
    }

    @Test
    void transfer_shouldFail_forSameAccount() {
        Long fromId = 1L, toId = 1L;
        AccountEntity from = AccountEntity.builder().id(fromId).customer(createCustomerEntity()).build();
        when(accountRepository.findById(fromId)).thenReturn(Optional.of(from));
        when(customerService.getCurrentCustomer()).thenReturn(createCustomer());
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> service.transfer(fromId, toId, 10.0, "me"));
        assertThat(ex.getMessage()).contains("Cannot transfer to the same account");
    }

    @Test
    void transfer_shouldFail_whenFromAccountNotFound() {
        Long fromId = 1L, toId = 2L;
        when(accountRepository.findById(fromId)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(
            EntityNotFoundException.class, () -> service.transfer(fromId, toId, 10.0, "me")
        );
        assertThat(ex.getMessage()).contains("Account not found");
        verify(transactionRepository, never()).saveAll(anyList());
        verify(accountRepository, never()).saveAll(anyList());
    }

    @Test
    void transfer_shouldFail_whenToAccountNotFound() {
        Long fromId = 1L, toId = 2L;
        AccountEntity from = AccountEntity.builder()
            .id(fromId).balance(100.0).overdraftLimit(-200.0).customer(createCustomerEntity()).currency(Currency.EUR).build();
        when(customerService.getCurrentCustomer()).thenReturn(createCustomer());
        when(accountRepository.findById(fromId)).thenReturn(Optional.of(from));
        when(accountRepository.findById(toId)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(
            EntityNotFoundException.class, () -> service.transfer(fromId, toId, 10.0, "me")
        );
        assertThat(ex.getMessage()).contains("To account not found");
        verify(transactionRepository, never()).saveAll(anyList());
        verify(accountRepository, never()).saveAll(anyList());
    }

    @Test
    void transfer_shouldFail_whenFromAccountNotOwned() {
        Long fromId = 1L, toId = 2L;
        AccountEntity from = AccountEntity.builder()
            .id(fromId).balance(100.0).overdraftLimit(-200.0).customer(createOtherCustomerEntity()).currency(Currency.EUR).build();
        AccountEntity to = AccountEntity.builder().id(toId).balance(50.0).overdraftLimit(-200.0).currency(Currency.EUR).build();
        when(customerService.getCurrentCustomer()).thenReturn(createCustomer());
        when(accountRepository.findById(fromId)).thenReturn(Optional.of(from));
        when(accountRepository.findById(toId)).thenReturn(Optional.of(to));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> service.transfer(fromId, toId, 10.0, "me"));
        assertThat(ex.getMessage()).contains("does not belong");
        verify(transactionRepository, never()).saveAll(anyList());
        verify(accountRepository, never()).saveAll(anyList());
    }

    @Test
    void transfer_shouldFail_whenOverdraftExceeded() {
        Long fromId = 1L, toId = 2L;
        CustomerEntity customerEntity = createCustomerEntity();
        AccountEntity from = AccountEntity.builder()
            .id(fromId).balance(0.0).overdraftLimit(-100.0).currency(Currency.EUR).customer(customerEntity).build();
        AccountEntity to = AccountEntity.builder().id(toId).balance(0.0).overdraftLimit(-100.0).currency(Currency.EUR).build();

        when(customerService.getCurrentCustomer()).thenReturn(createCustomer());
        when(accountRepository.findById(fromId)).thenReturn(Optional.of(from));
        when(accountRepository.findById(toId)).thenReturn(Optional.of(to));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.transfer(fromId, toId, 150.0, "me"));
        assertThat(ex.getMessage()).contains("overdraft");
        verify(transactionRepository, never()).saveAll(anyList());
        verify(accountRepository, never()).saveAll(anyList());
    }

    private CustomerEntity createCustomerEntity() {
        return new CustomerEntity(99L, "11111111-1111-1111-1111-111111111111", "Test", "t@test");
    }

    private CustomerEntity createOtherCustomerEntity() {
        return new CustomerEntity(1L, "22222222-22222222-22222222-22222222-22222222", "Test", "t@test");
    }

    private Customer createCustomer() {
        return new Customer(99L, "11111111-1111-1111-1111-111111111111", "Test", "t@test");
    }

}
