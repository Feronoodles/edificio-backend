package com.edificio.app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final Map<String, LoginWindow> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final long windowMinutes;

    public LoginRateLimitFilter(
            @Value("${app.security.login-rate-limit.max-attempts}") int maxAttempts,
            @Value("${app.security.login-rate-limit.window-minutes}") long windowMinutes
    ) {
        this.maxAttempts = maxAttempts;
        this.windowMinutes = windowMinutes;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isLoginRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        var key = clientKey(request);
        var now = Instant.now();
        var window = attempts.compute(key, (ignored, current) -> nextWindow(current, now));

        if (window.count() > maxAttempts) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("""
                    {"status":429,"error":"Too Many Requests","message":"Demasiados intentos de login. Intenta nuevamente mas tarde."}
                    """);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && "/api/auth/login".equals(request.getRequestURI());
    }

    private LoginWindow nextWindow(LoginWindow current, Instant now) {
        if (current == null || current.expiresAt().isBefore(now)) {
            cleanupExpired(now);
            return new LoginWindow(1, now.plus(windowMinutes, ChronoUnit.MINUTES));
        }
        return new LoginWindow(current.count() + 1, current.expiresAt());
    }

    private void cleanupExpired(Instant now) {
        attempts.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record LoginWindow(int count, Instant expiresAt) {
    }
}
