package com.eventalert.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory, fixed-window rate limiter, keyed by authenticated user or IP.
 * Single-instance only — resets on restart, not shared across instances.
 * A distributed store (e.g. Redis) would be the fix if this ever needs to scale out.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    private final long windowMillis;
    private final int authLimit;
    private final int defaultLimit;

    public RateLimitingFilter(@Value("${app.rate-limit.window-seconds:60}") long windowSeconds,
                               @Value("${app.rate-limit.auth-limit:10}") int authLimit,
                               @Value("${app.rate-limit.default-limit:120}") int defaultLimit) {
        this.windowMillis = windowSeconds * 1000;
        this.authLimit = authLimit;
        this.defaultLimit = defaultLimit;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String key = resolveKey(request);
        int limit = request.getRequestURI().startsWith("/api/auth/") ? authLimit : defaultLimit;

        if (isOverLimit(key, limit)) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded, try again shortly.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    // Keys by authenticated user when available (set by JwtAuthenticationFilter, which
    // runs before this one) so one user's traffic doesn't get lumped in with others
    // behind the same IP/NAT; falls back to remote address for anonymous requests
    // (registration, login) where there's no principal yet.
    @NonNull
    private String resolveKey(@NonNull HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            return "user:" + auth.getName();
        }
        return "ip:" + request.getRemoteAddr();
    }

    private boolean isOverLimit(@NonNull String key, int limit) {
        long now = System.currentTimeMillis();
        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStart >= windowMillis) {
                return new Window(now, new AtomicInteger(1));
            }
            existing.count().incrementAndGet();
            return existing;
        });
        return window.count().get() > limit;
    }

    /** One rate-limit window's start time and request count for a single client key. */
    private record Window(long windowStart, AtomicInteger count) {
    }
}
