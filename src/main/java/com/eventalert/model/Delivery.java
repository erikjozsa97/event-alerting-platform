package com.eventalert.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Delivery {

    private UUID id;
    private UUID alertRuleId;
    private UUID eventId; // nullable — null for manual test sends not tied to a real ingested event
    private UUID channelId;
    private DeliveryStatus status;
    private OffsetDateTime attemptedAt;
    private String errorMessage;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAlertRuleId() {
        return alertRuleId;
    }

    public void setAlertRuleId(UUID alertRuleId) {
        this.alertRuleId = alertRuleId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getChannelId() {
        return channelId;
    }

    public void setChannelId(UUID channelId) {
        this.channelId = channelId;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryStatus status) {
        this.status = status;
    }

    public OffsetDateTime getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(OffsetDateTime attemptedAt) {
        this.attemptedAt = attemptedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
