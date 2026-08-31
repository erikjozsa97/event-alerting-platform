package com.eventalert.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.lang.NonNull;

/**
 * Registration request. No dedicated view/DTO layer — request payloads live
 * here in model, next to the domain classes they map onto.
 *
 * @param email    the account email, must be unique
 * @param password plaintext password, hashed before storage
 * @param isAdmin  if true, the created account gets {@link Role#ADMIN} — see
 *                 the README for the security tradeoff this implies
 */
public record RegisterRequest(
        @NonNull @NotBlank @Email String email,
        @NonNull @NotBlank @Size(min = 8, max = 100) String password,
        boolean isAdmin
) {
}
