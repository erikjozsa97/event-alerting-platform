package com.eventalert.service;

import com.eventalert.exception.EmailAlreadyExistsException;
import com.eventalert.exception.InvalidCredentialsException;
import com.eventalert.model.AuthResponse;
import com.eventalert.model.LoginRequest;
import com.eventalert.model.RegisterRequest;
import com.eventalert.model.Role;
import com.eventalert.model.User;
import com.eventalert.repository.UserRepository;
import com.eventalert.security.JwtService;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Service managing user authentication, registration, and credential verification.
 * <p>
 * Handles account creation with password encoding, role assignment, and issuance of
 * JWT access tokens upon valid authentication attempts.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @NonNull
    public User register(@NonNull RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        // Self-service admin promotion at registration — see the README for the
        // security tradeoff this implies.
        user.setRole(request.isAdmin() ? Role.ADMIN : Role.USER);
        user.setEnabled(true);
        user.setCreatedAt(OffsetDateTime.now());

        return userRepository.save(user);
    }

    @NonNull
    public AuthResponse login(@NonNull LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return jwtService.issueToken(user);
    }
}
