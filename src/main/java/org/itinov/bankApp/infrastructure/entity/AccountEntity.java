package org.itinov.bankApp.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;
import org.itinov.bankApp.domain.enums.Currency;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a bank account with attributes such as account number, balance, overdraft limit, currency,
 * associated customer, and a list of transactions.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountEntity {
    @Id
    @GeneratedValue
    private Long id;

    private String number;
    private double balance;
    private double overdraftLimit;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    @ManyToOne
    private CustomerEntity customer;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TransactionEntity> transactions = new ArrayList<>();

}
