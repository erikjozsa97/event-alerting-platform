package com.eventalert.model;

import org.springframework.lang.NonNull;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A registered account — either a regular user or an admin, distinguished by
 * {@link Role}.
 */
public class User {

    private UUID id;
    private String email;

    // Controllers never return this model directly — com.eventalert.view.UserView
    // is what leaves the API, and it simply doesn't have this field. That's the
    // only thing preventing the hash from leaking, so don't add a serializer
    // that returns User (or any future model with a secret field) as-is.
    private String passwordHash;

    private Role role;
    private boolean enabled;
    private OffsetDateTime createdAt;

    @NonNull
    public UUID getId() {
        return id;
    }

    public void setId(@NonNull UUID id) {
        this.id = id;
    }

    @NonNull
    public String getEmail() {
        return email;
    }

    public void setEmail(@NonNull String email) {
        this.email = email;
    }

    @NonNull
    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(@NonNull String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @NonNull
    public Role getRole() {
        return role;
    }

    public void setRole(@NonNull Role role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @NonNull
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(@NonNull OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
