package com.eventalert.service;

import com.eventalert.exception.AlertRuleNotFoundException;
import com.eventalert.exception.ChannelNotFoundException;
import com.eventalert.exception.NoChannelsLinkedException;
import com.eventalert.model.AlertRule;
import com.eventalert.model.Channel;
import com.eventalert.model.Delivery;
import com.eventalert.model.DeliveryStatus;
import com.eventalert.model.Event;
import com.eventalert.model.NotificationMessage;
import com.eventalert.model.TestNotificationRequest;
import com.eventalert.model.User;
import com.eventalert.repository.AlertRuleRepository;
import com.eventalert.repository.ChannelRepository;
import com.eventalert.repository.DeliveryRepository;
import com.eventalert.repository.UserRepository;
import com.eventalert.security.CurrentUserService;
import com.eventalert.view.DeliveryView;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DeliveryService {

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 500;

    private final AlertRuleRepository alertRuleRepository;
    private final ChannelRepository channelRepository;
    private final DeliveryRepository deliveryRepository;
    private final UserRepository userRepository;
    private final NotificationChannelDispatcher notificationChannelDispatcher;
    private final CurrentUserService currentUserService;

    public DeliveryService(AlertRuleRepository alertRuleRepository,
                            ChannelRepository channelRepository,
                            DeliveryRepository deliveryRepository,
                            UserRepository userRepository,
                            NotificationChannelDispatcher notificationChannelDispatcher,
                            CurrentUserService currentUserService) {
        this.alertRuleRepository = alertRuleRepository;
        this.channelRepository = channelRepository;
        this.deliveryRepository = deliveryRepository;
        this.userRepository = userRepository;
        this.notificationChannelDispatcher = notificationChannelDispatcher;
        this.currentUserService = currentUserService;
    }

    // HTTP-triggered path — relies on CurrentUserService, which reads the
    // authenticated request's SecurityContext. Only safe to call from a controller.
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

    // Background-triggered path (called by MatchingService from the ingestion
    // scheduler) — there is no HTTP request or SecurityContext on that thread, so
    // this resolves the owning user directly from the rule instead of via
    // CurrentUserService. Shares the same retry/logging core as the path above.
    public List<Delivery> deliverForEvent(AlertRule rule, Event event) {
        List<UUID> channelIds = alertRuleRepository.findChannelIds(rule.getId());
        if (channelIds.isEmpty()) {
            return List.of();
        }

        User owner = userRepository.findById(rule.getUserId())
                .orElseThrow(() -> new IllegalStateException("Alert rule " + rule.getId()
                        + " references a missing user " + rule.getUserId()));

        NotificationMessage message = buildMessage(rule, event);

        List<Delivery> deliveries = new ArrayList<>();
        for (UUID channelId : channelIds) {
            channelRepository.findByIdAndUserId(channelId, owner.getId())
                    .ifPresent(channel -> deliveries.add(
                            dispatchWithRetry(rule.getId(), event.getId(), channel, owner, message)));
        }
        return deliveries;
    }

    public List<DeliveryView> listForRule(UUID alertRuleId) {
        User user = currentUserService.getCurrentUser();
        alertRuleRepository.findByIdAndUserId(alertRuleId, user.getId())
                .orElseThrow(() -> new AlertRuleNotFoundException(alertRuleId));
        return deliveryRepository.findByAlertRuleId(alertRuleId).stream()
                .map(DeliveryView::from)
                .toList();
    }

    private NotificationMessage buildMessage(AlertRule rule, Event event) {
        String title = "[" + rule.getCategory() + "] " + rule.getName();
        String body = event.getPayload().entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("\n"));
        return new NotificationMessage(title, body.isBlank() ? "(no details)" : body);
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
