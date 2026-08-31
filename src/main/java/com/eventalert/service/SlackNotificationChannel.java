package com.eventalert.service;

import com.eventalert.exception.NotificationDeliveryException;
import com.eventalert.model.Channel;
import com.eventalert.model.ChannelType;
import com.eventalert.model.NotificationMessage;
import com.eventalert.model.User;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class SlackNotificationChannel implements NotificationChannel {

    private final RestClient restClient = RestClient.create();

    @Override
    public ChannelType getType() {
        return ChannelType.SLACK;
    }

    @Override
    public void send(Channel channel, User recipient, NotificationMessage message) {
        Object urlRaw = channel.getConfig() == null ? null : channel.getConfig().get("webhookUrl");
        if (!(urlRaw instanceof String webhookUrl)) {
            throw new NotificationDeliveryException("Channel has no webhookUrl configured");
        }

        String text = "*" + message.title() + "*\n" + message.body();

        try {
            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", text))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new NotificationDeliveryException("Failed to post to Slack: " + e.getMessage(), e);
        }
    }
}
