package com.eventalert.view;

import com.eventalert.model.Delivery;
import com.eventalert.model.DeliveryStatus;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Admin-only view of a delivery — unlike {@link DeliveryView}, includes the
 * owning user so an admin can see whose delivery it is.
 */
public record AdminDeliveryView(
        @NonNull UUID id,
        @NonNull UUID alertRuleId,
        @Nullable UUID userId,
        @Nullable UUID eventId,
        @NonNull UUID channelId,
        @NonNull DeliveryStatus status,
        @NonNull OffsetDateTime attemptedAt,
        @Nullable String errorMessage
) {
    /**
     * Builds the admin-facing view of a delivery, attaching the owning user's id.
     */
    @NonNull
    public static AdminDeliveryView from(@NonNull Delivery delivery, @Nullable UUID userId) {
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
