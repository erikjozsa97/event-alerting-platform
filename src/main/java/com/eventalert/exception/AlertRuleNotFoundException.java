package com.eventalert.exception;

import org.springframework.lang.NonNull;

import java.util.UUID;

/**
 * Thrown when a lookup by id finds no alert rule owned by the caller.
 */
public class AlertRuleNotFoundException extends RuntimeException {

    public AlertRuleNotFoundException(@NonNull UUID id) {
        super("No alert rule found with id " + id);
    }
}
