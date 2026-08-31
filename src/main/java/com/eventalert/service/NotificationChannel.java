package com.eventalert.service;

import com.eventalert.model.Channel;
import com.eventalert.model.ChannelType;
import com.eventalert.model.NotificationMessage;
import com.eventalert.model.User;
import org.springframework.lang.NonNull;

/**
 * The extensibility abstraction from the original plan — one implementation
 * per {@link ChannelType}.
 */
public interface NotificationChannel {

    @NonNull
    ChannelType getType();

    /**
     * @throws com.eventalert.exception.NotificationDeliveryException if the send fails
     */
    void send(@NonNull Channel channel, @NonNull User recipient, @NonNull NotificationMessage message);
}
