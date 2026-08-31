package com.eventalert.model;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One send attempt of an alert rule's match through one channel — a single
 * row logs the final outcome (retries happen before this is written, not
 * across multiple rows).
 */
public class Delivery {

    private UUID id;
    private UUID alertRuleId;
    private UUID eventId; // nullable — null for manual test sends not tied to a real ingested event
    private UUID channelId;
    private DeliveryStatus status;
    private OffsetDateTime attemptedAt;
    private String errorMessage;

    @NonNull
    public UUID getId() {
        return id;
    }

    public void setId(@NonNull UUID id) {
        this.id = id;
    }

    @NonNull
    public UUID getAlertRuleId() {
        return alertRuleId;
    }

    public void setAlertRuleId(@NonNull UUID alertRuleId) {
        this.alertRuleId = alertRuleId;
    }

    @Nullable
    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(@Nullable UUID eventId) {
        this.eventId = eventId;
    }

    @NonNull
    public UUID getChannelId() {
        return channelId;
    }

    public void setChannelId(@NonNull UUID channelId) {
        this.channelId = channelId;
    }

    @NonNull
    public DeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(@NonNull DeliveryStatus status) {
        this.status = status;
    }

    @NonNull
    public OffsetDateTime getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(@NonNull OffsetDateTime attemptedAt) {
        this.attemptedAt = attemptedAt;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(@Nullable String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
