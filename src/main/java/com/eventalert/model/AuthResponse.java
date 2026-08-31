package com.eventalert.model;

import org.springframework.lang.NonNull;

/**
 * Response returned on successful login, carrying the issued JWT.
 */
public record AuthResponse(
        @NonNull String token,
        @NonNull String tokenType,
        long expiresInSeconds
) {
}
