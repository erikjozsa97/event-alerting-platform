package com.eventalert.exception;

import org.springframework.lang.NonNull;

/**
 * Thrown when a channel's config fails its type-specific validation.
 */
public class InvalidChannelConfigException extends RuntimeException {

    public InvalidChannelConfigException(@NonNull String message) {
        super(message);
    }
}
