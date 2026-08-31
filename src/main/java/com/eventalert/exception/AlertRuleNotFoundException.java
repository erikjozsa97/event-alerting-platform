package com.eventalert.exception;

import java.util.UUID;

public class AlertRuleNotFoundException extends RuntimeException {

    public AlertRuleNotFoundException(UUID id) {
        super("No alert rule found with id " + id);
    }
}
