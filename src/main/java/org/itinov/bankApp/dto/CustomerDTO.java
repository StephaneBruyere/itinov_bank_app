package org.itinov.bankApp.dto;

/**
 * Data Transfer Object representing a Customer.
 * Contains customer details and a list of associated accounts.
 */
public record CustomerDTO(
    Long id,
    String name,
    String email
) {
}
