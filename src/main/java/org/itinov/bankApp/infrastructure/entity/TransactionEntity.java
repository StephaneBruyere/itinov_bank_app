package org.itinov.bankApp.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;
import org.itinov.bankApp.domain.enums.Currency;
import org.itinov.bankApp.domain.enums.OperationType;

import java.time.LocalDateTime;

/**
 * Entity representing a Transaction in the banking application.
 * Each transaction is associated with an account and records details such as amount, type, currency,
 * and balance after the transaction.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEntity {
    @Id
    @GeneratedValue
    private Long id;

    private LocalDateTime date;
    private double amount;

    @Enumerated(EnumType.STRING)
    private OperationType type;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    private String performedBy;
    private double balanceAfter;

    @ManyToOne
    private AccountEntity account;
}
