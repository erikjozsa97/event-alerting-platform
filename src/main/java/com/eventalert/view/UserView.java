package com.eventalert.view;

import com.eventalert.model.Role;
import com.eventalert.model.User;
import org.springframework.lang.NonNull;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * What actually leaves the API for a {@link User} — notably, no password hash field.
 */
public record UserView(
        @NonNull UUID id,
        @NonNull String email,
        @NonNull Role role,
        boolean enabled,
        @NonNull OffsetDateTime createdAt
) {
    /**
     * Builds the public view of a user, omitting the password hash.
     */
    @NonNull
    public static UserView from(@NonNull User user) {
        return new UserView(user.getId(), user.getEmail(), user.getRole(), user.isEnabled(), user.getCreatedAt());
    }
}
