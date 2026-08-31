package com.eventalert.model;

import jakarta.validation.constraints.NotNull;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Map;

/**
 * Request to create a notification channel.
 *
 * @param type   which kind of channel to create
 * @param config channel-specific settings (e.g. a Slack webhook URL); may be
 *               omitted, in which case an empty config is used
 */
public record ChannelRequest(
        @NonNull @NotNull ChannelType type,
        @Nullable Map<String, Object> config
) {
}
