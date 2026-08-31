package com.eventalert.model;

import org.springframework.lang.NonNull;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * A normalized, deduped record ingested from an {@code EventSource}.
 */
public class Event {

    private UUID id;
    private String source;
    private String externalId;
    private Category category;
    private Map<String, Object> payload;
    private OffsetDateTime occurredAt;
    private OffsetDateTime ingestedAt;

    @NonNull
    public UUID getId() {
        return id;
    }

    public void setId(@NonNull UUID id) {
        this.id = id;
    }

    @NonNull
    public String getSource() {
        return source;
    }

    public void setSource(@NonNull String source) {
        this.source = source;
    }

    @NonNull
    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(@NonNull String externalId) {
        this.externalId = externalId;
    }

    @NonNull
    public Category getCategory() {
        return category;
    }

    public void setCategory(@NonNull Category category) {
        this.category = category;
    }

    @NonNull
    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(@NonNull Map<String, Object> payload) {
        this.payload = payload;
    }

    @NonNull
    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(@NonNull OffsetDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    @NonNull
    public OffsetDateTime getIngestedAt() {
        return ingestedAt;
    }

    public void setIngestedAt(@NonNull OffsetDateTime ingestedAt) {
        this.ingestedAt = ingestedAt;
    }
}
