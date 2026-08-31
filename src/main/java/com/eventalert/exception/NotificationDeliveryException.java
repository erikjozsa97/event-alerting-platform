package com.eventalert.exception;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Thrown when a single send through a {@code NotificationChannel} fails. Caught
 * internally by the delivery retry loop — never reaches a controller, so it has
 * no {@code GlobalExceptionHandler} mapping.
 */
public class NotificationDeliveryException extends RuntimeException {

    public NotificationDeliveryException(@NonNull String message) {
        super(message);
    }

    public NotificationDeliveryException(@NonNull String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
