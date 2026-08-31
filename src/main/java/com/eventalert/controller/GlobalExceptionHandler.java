package com.eventalert.controller;

import com.eventalert.exception.AlertRuleNotFoundException;
import com.eventalert.exception.ChannelNotFoundException;
import com.eventalert.exception.EmailAlreadyExistsException;
import com.eventalert.exception.InvalidChannelConfigException;
import com.eventalert.exception.InvalidCredentialsException;
import com.eventalert.exception.InvalidCriteriaException;
import com.eventalert.exception.NoChannelsLinkedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralized exception handler for REST controllers.
 * <p>
 * Intercepts domain-specific and framework exceptions across the application
 * and formats them into a standardized JSON response body containing metadata
 * such as timestamp, HTTP status code, reason phrase, and message.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    @NonNull
    public ResponseEntity<Map<String, Object>> handleEmailExists(@NonNull EmailAlreadyExistsException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @NonNull
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(@NonNull InvalidCredentialsException ex) {
        return error(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler({InvalidCriteriaException.class, InvalidChannelConfigException.class,
            NoChannelsLinkedException.class})
    @NonNull
    public ResponseEntity<Map<String, Object>> handleInvalidPayload(@NonNull RuntimeException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler({AlertRuleNotFoundException.class, ChannelNotFoundException.class})
    @NonNull
    public ResponseEntity<Map<String, Object>> handleNotFound(@NonNull RuntimeException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @NonNull
    public ResponseEntity<Map<String, Object>> handleValidation(@NonNull MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @NonNull
    private ResponseEntity<Map<String, Object>> error(@NonNull HttpStatus status, @NonNull String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
