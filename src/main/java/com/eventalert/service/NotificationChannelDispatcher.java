package com.eventalert.service;

import com.eventalert.model.ChannelType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class NotificationChannelDispatcher {

    private final Map<ChannelType, NotificationChannel> channelsByType;

    public NotificationChannelDispatcher(List<NotificationChannel> channels) {
        this.channelsByType = channels.stream()
                .collect(Collectors.toMap(NotificationChannel::getType, c -> c));
    }

    public NotificationChannel get(ChannelType type) {
        NotificationChannel channel = channelsByType.get(type);
        if (channel == null) {
            throw new IllegalStateException("No NotificationChannel registered for type " + type);
        }
        return channel;
    }
}
