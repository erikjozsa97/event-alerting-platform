package com.eventalert.service;

import com.eventalert.model.ChannelType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dispatcher component that resolves the appropriate {@link NotificationChannel} implementation for a given channel type.
 * <p>
 * Automatically collects all Spring-managed {@link NotificationChannel} beans at application startup
 * and maps them by their supported {@link ChannelType}.
 */
@Component
public class NotificationChannelDispatcher {

    private final Map<ChannelType, NotificationChannel> channelsByType;

    public NotificationChannelDispatcher(@NonNull List<NotificationChannel> channels) {
        this.channelsByType = channels.stream()
                .collect(Collectors.toMap(NotificationChannel::getType, c -> c));
    }

    @NonNull
    public NotificationChannel get(@NonNull ChannelType type) {
        NotificationChannel channel = channelsByType.get(type);
        if (channel == null) {
            throw new IllegalStateException("No NotificationChannel registered for type " + type);
        }
        return channel;
    }
}
