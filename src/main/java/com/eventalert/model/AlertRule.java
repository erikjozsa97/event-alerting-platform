package com.eventalert.model;

import org.springframework.lang.NonNull;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * A user-owned rule: watch a {@link Category} for events matching
 * {@code criteria}, and notify linked channels when one matches.
 */
public class AlertRule {

    private UUID id;
    private UUID userId;
    private Category category;
    private String name;
    private Map<String, Object> criteria;
    private boolean active;
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
    public Category getCategory() {
        return category;
    }

    public void setCategory(@NonNull Category category) {
        this.category = category;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    public Map<String, Object> getCriteria() {
        return criteria;
    }

    public void setCriteria(@NonNull Map<String, Object> criteria) {
        this.criteria = criteria;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @NonNull
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(@NonNull OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
