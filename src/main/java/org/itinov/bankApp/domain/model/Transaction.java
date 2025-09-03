package org.itinov.bankApp.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.itinov.bankApp.domain.model.enums.Currency;
import org.itinov.bankApp.domain.model.enums.OperationType;

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
public class Transaction {
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
    private Account account;
}
