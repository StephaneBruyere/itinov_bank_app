package org.itinov.bankApp.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for getting a token via username/password.
 */
public record AuthRequest(
    @NotBlank(message = "username is required")
    String username,
    @NotBlank(message = "password is required")
    String password
) {}
