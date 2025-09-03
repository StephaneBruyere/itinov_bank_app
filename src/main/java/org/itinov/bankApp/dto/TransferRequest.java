package org.itinov.bankApp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request body for a transfer operation.
 */
public record TransferRequest(
    @NotNull(message = "toAccountId is required")
    Long toAccountId,
    @NotNull(message = "amount is required")
    @Positive(message = "amount must be greater than 0")
    Double amount
) {}
