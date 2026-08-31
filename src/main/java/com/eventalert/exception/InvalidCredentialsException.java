package com.eventalert.exception;

/**
 * Thrown on a failed login attempt (unknown email, wrong password, or disabled account).
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
