package com.eventalert.security;

import com.eventalert.model.User;
import com.eventalert.repository.UserRepository;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Resolves the authenticated user for user-scoped endpoints, from the
 * current request's {@link SecurityContextHolder}.
 */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(@NonNull UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @NonNull
    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
    }

    @NonNull
    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
