package com.eventalert.view;

import com.eventalert.model.Channel;
import com.eventalert.model.ChannelType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * What actually leaves the API for a {@link Channel} — masks the Slack
 * webhook URL inside {@code config}, since that's effectively a bearer
 * credential rather than harmless configuration.
 */
public record ChannelView(
        @NonNull UUID id,
        @NonNull ChannelType type,
        @NonNull Map<String, Object> config,
        boolean verified,
        @NonNull OffsetDateTime createdAt
) {
    /**
     * Builds the owner-facing view of a channel, masking any sensitive config.
     */
    @NonNull
    public static ChannelView from(@NonNull Channel channel) {
        return new ChannelView(
                channel.getId(),
                channel.getType(),
                maskConfig(channel.getType(), channel.getConfig()),
                channel.isVerified(),
                channel.getCreatedAt()
        );
    }

    // A Slack webhook URL is a bearer credential — anyone with it can post to the
    // channel — so it's masked here rather than trusted to @JsonIgnore or the
    // caller's discretion. This is exactly the kind of leak the view layer exists
    // to prevent even when a field is nested inside a generic Map.
    @NonNull
    private static Map<String, Object> maskConfig(@NonNull ChannelType type, @Nullable Map<String, Object> config) {
        if (config == null || config.isEmpty()) {
            return Map.of();
        }
        if (type == ChannelType.SLACK && config.get("webhookUrl") instanceof String url) {
            Map<String, Object> masked = new LinkedHashMap<>(config);
            masked.put("webhookUrl", maskUrl(url));
            return masked;
        }
        return config;
    }

    @NonNull
    private static String maskUrl(@NonNull String url) {
        if (url.length() <= 12) {
            return "****";
        }
        return url.substring(0, 8) + "..." + url.substring(url.length() - 4);
    }
}
