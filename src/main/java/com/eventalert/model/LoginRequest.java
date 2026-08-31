package com.eventalert.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.lang.NonNull;

/**
 * Login request — email and plaintext password to verify against the stored hash.
 */
public record LoginRequest(
        @NonNull @NotBlank @Email String email,
        @NonNull @NotBlank String password
) {
}
