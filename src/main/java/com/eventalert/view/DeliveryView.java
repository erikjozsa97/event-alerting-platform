package com.eventalert.view;

import com.eventalert.model.Delivery;
import com.eventalert.model.DeliveryStatus;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * What actually leaves the API for a {@link Delivery}, scoped to the owner
 * of the alert rule it belongs to.
 */
public record DeliveryView(
        @NonNull UUID id,
        @NonNull UUID alertRuleId,
        @Nullable UUID eventId,
        @NonNull UUID channelId,
        @NonNull DeliveryStatus status,
        @NonNull OffsetDateTime attemptedAt,
        @Nullable String errorMessage
) {
    /**
     * Builds the owner-facing view of a delivery attempt.
     */
    @NonNull
    public static DeliveryView from(@NonNull Delivery delivery) {
        return new DeliveryView(
                delivery.getId(),
                delivery.getAlertRuleId(),
                delivery.getEventId(),
                delivery.getChannelId(),
                delivery.getStatus(),
                delivery.getAttemptedAt(),
                delivery.getErrorMessage()
        );
    }
}
