package com.eventalert.service;

import com.eventalert.exception.InvalidChannelConfigException;
import com.eventalert.model.ChannelType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validator implementation for Slack notification channel configurations.
 * <p>
 * Validates configuration maps for the {@link ChannelType#SLACK} channel type, ensuring that
 * a valid {@code webhookUrl} parameter is provided and conforms to standard Slack webhook URL patterns.
 */
@Component
public class SlackChannelConfigValidator implements ChannelConfigValidator {

    private static final Pattern WEBHOOK_PATTERN =
            Pattern.compile("^https://hooks\\.slack\\.com/services/.+");

    @Override
    @NonNull
    public ChannelType supports() {
        return ChannelType.SLACK;
    }

    @Override
    public void validate(@NonNull Map<String, Object> config) {
        Object urlRaw = config.get("webhookUrl");
        if (!(urlRaw instanceof String url) || !WEBHOOK_PATTERN.matcher(url).matches()) {
            throw new InvalidChannelConfigException(
                    "SLACK channels require a 'webhookUrl' matching https://hooks.slack.com/services/...");
        }
    }
}
