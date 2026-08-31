package com.eventalert.exception;

import org.springframework.lang.NonNull;

/**
 * Thrown when an alert rule's criteria fails its category-specific validation.
 */
public class InvalidCriteriaException extends RuntimeException {

    public InvalidCriteriaException(@NonNull String message) {
        super(message);
    }
}
