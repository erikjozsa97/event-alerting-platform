package com.eventalert.view;

import com.eventalert.model.Delivery;
import com.eventalert.model.DeliveryStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryView(
        UUID id,
        UUID alertRuleId,
        UUID channelId,
        DeliveryStatus status,
        OffsetDateTime attemptedAt,
        String errorMessage
) {
    public static DeliveryView from(Delivery delivery) {
        return new DeliveryView(
                delivery.getId(),
                delivery.getAlertRuleId(),
                delivery.getChannelId(),
                delivery.getStatus(),
                delivery.getAttemptedAt(),
                delivery.getErrorMessage()
        );
    }
}
