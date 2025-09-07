package org.itinov.bankApp.domain.model;

import lombok.Builder;
import org.itinov.bankApp.domain.enums.Currency;
import org.itinov.bankApp.domain.enums.OperationType;

import java.time.LocalDateTime;

@Builder
public record Transaction(
    Long id,
    LocalDateTime date,
    double amount,
    OperationType type,
    Currency currency,
    String performedBy,
    double balanceAfter,
    Account account
){
}
