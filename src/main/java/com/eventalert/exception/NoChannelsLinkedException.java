package com.eventalert.exception;

import java.util.UUID;

public class NoChannelsLinkedException extends RuntimeException {

    public NoChannelsLinkedException(UUID alertRuleId) {
        super("Alert rule " + alertRuleId + " has no channels linked to notify");
    }
}
