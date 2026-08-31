package com.eventalert.service;

import com.eventalert.model.ChannelType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Validator implementation for email notification channel configurations.
 * <p>
 * Validates configuration maps for the {@link ChannelType#EMAIL} channel type. Currently acts
 * as a no-op validator because email notifications default to sending directly to the user's
 * registered account address.
 */
@Component
public class EmailChannelConfigValidator implements ChannelConfigValidator {

    @Override
    @NonNull
    public ChannelType supports() {
        return ChannelType.EMAIL;
    }

    @Override
    public void validate(@NonNull  Map<String, Object> config) {
        // No required fields today — an EMAIL channel just sends to the account's
        // own address. Kept as an explicit (no-op) validator, rather than skipping
        // EMAIL entirely, so a future "send to a different address" option has an
        // obvious home instead of being bolted on ad hoc.
    }
}
