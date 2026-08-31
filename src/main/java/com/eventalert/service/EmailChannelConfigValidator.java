package com.eventalert.service;

import com.eventalert.model.ChannelType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EmailChannelConfigValidator implements ChannelConfigValidator {

    @Override
    public ChannelType supports() {
        return ChannelType.EMAIL;
    }

    @Override
    public void validate(Map<String, Object> config) {
        // No required fields today — an EMAIL channel just sends to the account's
        // own address. Kept as an explicit (no-op) validator, rather than skipping
        // EMAIL entirely, so a future "send to a different address" option has an
        // obvious home instead of being bolted on ad hoc.
    }
}
