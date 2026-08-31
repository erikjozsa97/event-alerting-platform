package com.eventalert.exception;

import java.util.UUID;

public class ChannelNotFoundException extends RuntimeException {

    public ChannelNotFoundException(UUID id) {
        super("No channel found with id " + id);
    }
}
