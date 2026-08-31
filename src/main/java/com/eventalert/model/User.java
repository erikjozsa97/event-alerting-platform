package com.eventalert.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.OffsetDateTime;
import java.util.UUID;

public class User {

    private UUID id;
    private String email;

    // No view/DTO layer in this project, so this model is returned directly
    // from controllers. @JsonIgnore is the safety net that would otherwise
    // be the view layer's job — without it, every endpoint that returns a
    // User (register now, admin user listing later) would leak the hash.
    @JsonIgnore
    private String passwordHash;

    private Role role;
    private boolean enabled;
    private OffsetDateTime createdAt;

    public User() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
