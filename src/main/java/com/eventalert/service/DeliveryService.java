package com.eventalert.service;

import com.eventalert.exception.AlertRuleNotFoundException;
import com.eventalert.exception.ChannelNotFoundException;
import com.eventalert.exception.NoChannelsLinkedException;
import com.eventalert.model.AlertRule;
import com.eventalert.model.Channel;
import com.eventalert.model.Delivery;
import com.eventalert.model.DeliveryStatus;
import com.eventalert.model.NotificationMessage;
import com.eventalert.model.TestNotificationRequest;
import com.eventalert.model.User;
import com.eventalert.repository.AlertRuleRepository;
import com.eventalert.repository.ChannelRepository;
import com.eventalert.repository.DeliveryRepository;
import com.eventalert.security.CurrentUserService;
import com.eventalert.view.DeliveryView;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DeliveryService {

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 500;

    private final AlertRuleRepository alertRuleRepository;
    private final ChannelRepository channelRepository;
    private final DeliveryRepository deliveryRepository;
    private final NotificationChannelDispatcher notificationChannelDispatcher;
    private final CurrentUserService currentUserService;

    public DeliveryService(AlertRuleRepository alertRuleRepository,
                            ChannelRepository channelRepository,
                            DeliveryRepository deliveryRepository,
                            NotificationChannelDispatcher notificationChannelDispatcher,
                            CurrentUserService currentUserService) {
        this.alertRuleRepository = alertRuleRepository;
        this.channelRepository = channelRepository;
        this.deliveryRepository = deliveryRepository;
        this.notificationChannelDispatcher = notificationChannelDispatcher;
        this.currentUserService = currentUserService;
    }

    // Manual trigger, ahead of M4's real event ingestion — lets the whole send
    // path (validation -> dispatch -> retry -> delivery log) be exercised via
    // Postman today instead of waiting for an event pipeline to exist.
    public List<DeliveryView> sendTestNotification(UUID alertRuleId, TestNotificationRequest request) {
        User user = currentUserService.getCurrentUser();

        AlertRule rule = alertRuleRepository.findByIdAndUserId(alertRuleId, user.getId())
                .orElseThrow(() -> new AlertRuleNotFoundException(alertRuleId));

        List<UUID> channelIds = alertRuleRepository.findChannelIds(alertRuleId);
        if (channelIds.isEmpty()) {
            throw new NoChannelsLinkedException(alertRuleId);
        }

        NotificationMessage message = new NotificationMessage(request.title(), request.body());

        List<DeliveryView> results = new ArrayList<>();
        for (UUID channelId : channelIds) {
            Channel channel = channelRepository.findByIdAndUserId(channelId, user.getId())
                    .orElseThrow(() -> new ChannelNotFoundException(channelId));
            Delivery delivery = dispatchWithRetry(rule.getId(), null, channel, user, message);
            results.add(DeliveryView.from(delivery));
        }
        return results;
    }

    private Delivery dispatchWithRetry(UUID alertRuleId, UUID eventId, Channel channel, User recipient,
                                        NotificationMessage message) {
        NotificationChannel sender = notificationChannelDispatcher.get(channel.getType());

        String lastError = null;
        boolean sent = false;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS && !sent; attempt++) {
            try {
                sender.send(channel, recipient, message);
                sent = true;
            } catch (Exception e) {
                lastError = e.getMessage();
                if (attempt < MAX_ATTEMPTS) {
                    sleep(RETRY_DELAY_MS * attempt);
                }
            }
        }

        Delivery delivery = new Delivery();
        delivery.setId(UUID.randomUUID());
        delivery.setAlertRuleId(alertRuleId);
        delivery.setEventId(eventId);
        delivery.setChannelId(channel.getId());
        delivery.setStatus(sent ? DeliveryStatus.SENT : DeliveryStatus.FAILED);
        delivery.setAttemptedAt(OffsetDateTime.now());
        delivery.setErrorMessage(sent ? null : lastError);

        return deliveryRepository.save(delivery);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
