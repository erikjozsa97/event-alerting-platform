package com.eventalert.exception;

import org.springframework.lang.NonNull;

import java.util.UUID;

/**
 * Thrown when trying to notify through an alert rule that has no channels linked.
 */
public class NoChannelsLinkedException extends RuntimeException {

    public NoChannelsLinkedException(@NonNull UUID alertRuleId) {
        super("Alert rule " + alertRuleId + " has no channels linked to notify");
    }
}
