package com.eventalert.view;

import com.eventalert.model.Delivery;
import com.eventalert.model.DeliveryStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminDeliveryView(
        UUID id,
        UUID alertRuleId,
        UUID userId,
        UUID eventId,
        UUID channelId,
        DeliveryStatus status,
        OffsetDateTime attemptedAt,
        String errorMessage
) {
    public static AdminDeliveryView from(Delivery delivery, UUID userId) {
        return new AdminDeliveryView(
                delivery.getId(),
                delivery.getAlertRuleId(),
                userId,
                delivery.getEventId(),
                delivery.getChannelId(),
                delivery.getStatus(),
                delivery.getAttemptedAt(),
                delivery.getErrorMessage()
        );
    }
}
