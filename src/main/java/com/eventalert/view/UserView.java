package com.eventalert.view;

import com.eventalert.model.Role;
import com.eventalert.model.User;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserView(
        UUID id,
        String email,
        Role role,
        boolean enabled,
        OffsetDateTime createdAt
) {
    public static UserView from(User user) {
        return new UserView(user.getId(), user.getEmail(), user.getRole(), user.isEnabled(), user.getCreatedAt());
    }
}
