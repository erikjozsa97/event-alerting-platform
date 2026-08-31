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
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Service managing notification dispatch and delivery execution for alert rules.
 * <p>
 * Supports both synchronous HTTP-triggered test dispatches and asynchronous background
 * event dispatches with retry capabilities, delivery logging, and metrics recording.
 */
@Service
public class DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 500;

    private final AlertRuleRepository alertRuleRepository;
    private final ChannelRepository channelRepository;
    private final DeliveryRepository deliveryRepository;
    private final UserRepository userRepository;
    private final NotificationChannelDispatcher notificationChannelDispatcher;
    private final CurrentUserService currentUserService;
    private final MeterRegistry meterRegistry;
    private final Executor deliveryExecutor;

    public DeliveryService(AlertRuleRepository alertRuleRepository,
                            ChannelRepository channelRepository,
                            DeliveryRepository deliveryRepository,
                            UserRepository userRepository,
                            NotificationChannelDispatcher notificationChannelDispatcher,
                            CurrentUserService currentUserService,
                            MeterRegistry meterRegistry,
                            @Qualifier("deliveryExecutor") Executor deliveryExecutor) {
        this.alertRuleRepository = alertRuleRepository;
        this.channelRepository = channelRepository;
        this.deliveryRepository = deliveryRepository;
        this.userRepository = userRepository;
        this.notificationChannelDispatcher = notificationChannelDispatcher;
        this.currentUserService = currentUserService;
        this.meterRegistry = meterRegistry;
        this.deliveryExecutor = deliveryExecutor;
    }

    // HTTP-triggered path — relies on CurrentUserService, which reads the
    // authenticated request's SecurityContext. Only safe to call from a controller.
    // Stays synchronous: the caller (Postman, a real client) expects the delivery
    // results back in the response body, not a "check back later" flow.
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
    // scheduler thread). Unlike sendTestNotification, this is fire-and-forget: each
    // channel's dispatch (up to MAX_ATTEMPTS with backoff) runs on deliveryExecutor
    // instead of blocking the scheduler, which needs to move on to the next event/
    // source rather than stall behind a slow or failing channel. Callers were already
    // ignoring the return value before this change, so making it void is not a
    // behavior change for MatchingService.
    public void deliverForEvent(@NonNull AlertRule rule, Event event) {
        List<UUID> channelIds = alertRuleRepository.findChannelIds(rule.getId());
        if (channelIds.isEmpty()) {
            return;
        }

        User owner = userRepository.findById(rule.getUserId())
                .orElseThrow(() -> new IllegalStateException("Alert rule " + rule.getId()
                        + " references a missing user " + rule.getUserId()));

        NotificationMessage message = buildMessage(rule, event);

        for (UUID channelId : channelIds) {
            channelRepository.findByIdAndUserId(channelId, owner.getId())
                    .ifPresent(channel -> deliveryExecutor.execute(() -> {
                        try {
                            dispatchWithRetry(rule.getId(), event.getId(), channel, owner, message);
                        } catch (Exception e) {
                            log.warn("Delivery dispatch failed for rule {} channel {}: {}",
                                    rule.getId(), channel.getId(), e.getMessage());
                        }
                    }));
        }
    }

    public List<DeliveryView> listForRule(UUID alertRuleId) {
        User user = currentUserService.getCurrentUser();
        alertRuleRepository.findByIdAndUserId(alertRuleId, user.getId())
                .orElseThrow(() -> new AlertRuleNotFoundException(alertRuleId));
        return deliveryRepository.findByAlertRuleId(alertRuleId).stream()
                .map(DeliveryView::from)
                .toList();
    }

    @NonNull
    private NotificationMessage buildMessage(@NonNull AlertRule rule, @NonNull Event event) {
        String title = "[" + rule.getCategory() + "] " + rule.getName();
        String body = event.getPayload().entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("\n"));
        return new NotificationMessage(title, body.isBlank() ? "(no details)" : body);
    }

    @NonNull
    private Delivery dispatchWithRetry(@NonNull UUID alertRuleId, @Nullable UUID eventId, @NonNull Channel channel, @NonNull User recipient,
                                       @NonNull NotificationMessage message) {
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

        deliveryRepository.save(delivery);

        meterRegistry.counter("deliveries.total",
                "channel", channel.getType().name(),
                "status", delivery.getStatus().name()
        ).increment();

        return delivery;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
