package com.app.Genderize.config.auth;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        String key;
        int limit;

        if (path.startsWith("/auth/")) {
            key = "IP:" + request.getRemoteAddr();
            limit = 10;
        } else {

            UUID userId = extractUserIdSafely(request);

            if (userId != null) {
                key = "USER:" + userId;
            } else {
                key = "ANON:" + request.getRemoteAddr();
            }

            limit = 60;
        }
        Bucket bucket = buckets.computeIfAbsent(key, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.builder()
                                .capacity(limit)
                                .refillGreedy(limit, Duration.ofMinutes(1))
                                .build())
                        .build()
        );

        if (!bucket.tryConsume(1)) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":\"error\",\"message\":\"Too many requests\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private UUID extractUserIdSafely(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");

        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                return jwtService.extractUserId(auth.substring(7));
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
