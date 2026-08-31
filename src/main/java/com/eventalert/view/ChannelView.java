package com.eventalert.view;

import com.eventalert.model.Channel;
import com.eventalert.model.ChannelType;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record ChannelView(
        UUID id,
        ChannelType type,
        Map<String, Object> config,
        boolean verified,
        OffsetDateTime createdAt
) {
    public static ChannelView from(Channel channel) {
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
    private static Map<String, Object> maskConfig(ChannelType type, Map<String, Object> config) {
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

    private static String maskUrl(String url) {
        if (url.length() <= 12) {
            return "****";
        }
        return url.substring(0, 8) + "..." + url.substring(url.length() - 4);
    }
}
