package com.eventalert.model;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInSeconds
) {
}
