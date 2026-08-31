package com.eventalert.exception;

import org.springframework.lang.NonNull;

import java.util.UUID;

/**
 * Thrown when a lookup by id finds no channel owned by the caller.
 */
public class ChannelNotFoundException extends RuntimeException {

    public ChannelNotFoundException(@NonNull UUID id) {
        super("No channel found with id " + id);
    }
}
