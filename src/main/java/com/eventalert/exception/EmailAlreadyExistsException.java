package com.eventalert.exception;

import org.springframework.lang.NonNull;

/**
 * Thrown on registration when the email is already taken.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(@NonNull String email) {
        super("An account with email '" + email + "' already exists");
    }
}
