package com.eventalert.model;

import jakarta.validation.constraints.NotBlank;
import org.springframework.lang.NonNull;

/**
 * Request payload for manually triggering a test notification on an alert rule.
 */
public record TestNotificationRequest(
        @NonNull @NotBlank String title,
        @NonNull @NotBlank String body
) {
}
