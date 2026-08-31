package com.eventalert.model;

import jakarta.validation.constraints.NotBlank;

public record TestNotificationRequest(
        @NotBlank String title,
        @NotBlank String body
) {
}
