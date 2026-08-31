package com.eventalert.service;

import com.eventalert.model.Channel;
import com.eventalert.model.ChannelType;
import com.eventalert.model.NotificationMessage;
import com.eventalert.model.User;

public interface NotificationChannel {

    ChannelType getType();

    /**
     * @throws com.eventalert.exception.NotificationDeliveryException if the send fails
     */
    void send(Channel channel, User recipient, NotificationMessage message);
}
