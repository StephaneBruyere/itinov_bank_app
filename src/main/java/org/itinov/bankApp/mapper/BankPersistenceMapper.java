package org.itinov.bankApp.mapper;

import org.itinov.bankApp.domain.model.Account;
import org.itinov.bankApp.domain.model.Customer;
import org.itinov.bankApp.domain.model.Transaction;
import org.itinov.bankApp.infrastructure.entity.AccountEntity;
import org.itinov.bankApp.infrastructure.entity.CustomerEntity;
import org.itinov.bankApp.infrastructure.entity.TransactionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

/**
 * Mapper interface for converting between domain models and DTOs.
 * Uses MapStruct to generate the implementation at compile time.
 */
@Mapper(componentModel = "spring")
public interface BankPersistenceMapper {

    Customer toDomain(CustomerEntity customer);

    List<Customer> toDomainCustomers(List<CustomerEntity> customers);

    // Mapping "complet" d'un compte: inclut ses transactions,
    // mais ces dernières utiliseront un mapping de transaction
    // qui ne remappe pas des comptes "complets" (voir plus bas).
    Account toDomain(AccountEntity account);

    List<Account> toDomainAccounts(List<AccountEntity> accounts);

    // Mapping "léger" d'un compte: pas de transactions -> casse la récursion
    @Named("accountShallow")
    @Mapping(target = "transactions", ignore = true)
    Account toShallow(AccountEntity account);

    // Lorsque l'on mappe une transaction,
    // on ne doit pas remapper des comptes complets (sinon boucle infinie).
    // On force l'utilisation du mapping "shallow" pour account.
    @Mapping(target = "account", qualifiedByName = "accountShallow")
    Transaction toDomain(TransactionEntity transaction);

    List<Transaction> toDomainTransactions(List<TransactionEntity> transactions);

}
