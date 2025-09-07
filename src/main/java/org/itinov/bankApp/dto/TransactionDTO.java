package org.itinov.bankApp.dto;

import org.itinov.bankApp.domain.enums.Currency;
import org.itinov.bankApp.domain.enums.OperationType;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing a Transaction.
 * Contains transaction details including type, amount, date, and associated account.
 */
public record TransactionDTO(
    Long id,
    LocalDateTime date,
    double amount,
    OperationType type,
    Currency currency,
    String performedBy,
    double balanceAfter,
    AccountDTO account
) {
}
