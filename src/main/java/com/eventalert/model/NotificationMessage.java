package com.eventalert.model;

import org.springframework.lang.NonNull;

/**
 * Generic carrier for what a notification says, independent of which channel
 * it's sent through.
 */
public record NotificationMessage(
        @NonNull String title,
        @NonNull String body
) {
}
