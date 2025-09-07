package org.itinov.bankApp.domain.model;

import lombok.Builder;
import org.itinov.bankApp.domain.enums.Currency;

import java.util.List;

@Builder
public record Account(
    Long id,
    String number,
    double balance,
    double overdraftLimit,
    Currency currency,
    Customer customer,
    List<Transaction> transactions
) {
}
