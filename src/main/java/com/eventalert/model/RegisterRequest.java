package com.eventalert.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// No dedicated view/DTO layer — request payloads live here in model,
// next to the domain classes they map onto.
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        boolean isAdmin
) {
}
