package org.itinov.bankApp.dto;

import org.itinov.bankApp.domain.enums.Currency;

import java.util.List;

/**
 * Data Transfer Object representing an Account.
 * Contains account details and a list of associated transactions.
 */
public record AccountDTO(
    Long id,
    String number,
    double balance,
    Currency currency,
    List<TransactionDTO> transactions
) {
}
