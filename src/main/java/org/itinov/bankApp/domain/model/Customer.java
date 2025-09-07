package org.itinov.bankApp.domain.model;

import lombok.Builder;

@Builder
public record Customer(
    Long id,
    String keycloakId,
    String name,
    String email
) {
}
