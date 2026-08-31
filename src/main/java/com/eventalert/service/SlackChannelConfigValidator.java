package com.eventalert.service;

import com.eventalert.exception.InvalidChannelConfigException;
import com.eventalert.model.ChannelType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

@Component
public class SlackChannelConfigValidator implements ChannelConfigValidator {

    private static final Pattern WEBHOOK_PATTERN =
            Pattern.compile("^https://hooks\\.slack\\.com/services/.+");

    @Override
    public ChannelType supports() {
        return ChannelType.SLACK;
    }

    @Override
    public void validate(Map<String, Object> config) {
        Object urlRaw = config == null ? null : config.get("webhookUrl");
        if (!(urlRaw instanceof String url) || !WEBHOOK_PATTERN.matcher(url).matches()) {
            throw new InvalidChannelConfigException(
                    "SLACK channels require a 'webhookUrl' matching https://hooks.slack.com/services/...");
        }
    }
}
