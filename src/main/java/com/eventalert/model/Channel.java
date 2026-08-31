package com.eventalert.model;

import org.springframework.lang.NonNull;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * A user-owned notification destination — an email address (implicitly the
 * account's own) or a Slack webhook, per {@link ChannelType}.
 */
public class Channel {

    private UUID id;
    private UUID userId;
    private ChannelType type;
    private Map<String, Object> config;
    private boolean verified;
    private OffsetDateTime createdAt;

    @NonNull
    public UUID getId() {
        return id;
    }

    public void setId(@NonNull UUID id) {
        this.id = id;
    }

    @NonNull
    public UUID getUserId() {
        return userId;
    }

    public void setUserId(@NonNull UUID userId) {
        this.userId = userId;
    }

    @NonNull
    public ChannelType getType() {
        return type;
    }

    public void setType(@NonNull ChannelType type) {
        this.type = type;
    }

    @NonNull
    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(@NonNull Map<String, Object> config) {
        this.config = config;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    @NonNull
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(@NonNull OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
