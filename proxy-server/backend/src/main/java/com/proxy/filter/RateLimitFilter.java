package com.proxy.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${proxy.rate-limit.requests-per-minute:100}")
    private int requestsPerMinute;

    @Value("${proxy.rate-limit.burst-capacity:20}")
    private int burstCapacity;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.classic(
                requestsPerMinute,
                Refill.greedy(requestsPerMinute, Duration.ofMinutes(1))
        );
        Bandwidth burst = Bandwidth.classic(
                burstCapacity,
                Refill.intervally(burstCapacity, Duration.ofSeconds(10))
        );
        return Bucket.builder()
                .addLimit(limit)
                .addLimit(burst)
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Skip rate limiting for health checks
        if (request.getRequestURI().contains("/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(clientIp, k -> createBucket());

        var probe = bucket.tryConsumeAndReturnRemaining(1);

        // Add rate limit headers
        response.addHeader("X-RateLimit-Limit", String.valueOf(requestsPerMinute));
        response.addHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
        response.addHeader("X-RateLimit-Reset", String.valueOf(
                System.currentTimeMillis() / 1000 + probe.getNanosToWaitForRefill() / 1_000_000_000));

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
        } else {
            long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            log.warn("Rate limit exceeded for IP: {} — retry in {}s", clientIp, waitSeconds);
            response.addHeader("Retry-After", String.valueOf(waitSeconds));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(String.format(
                    "{\"error\": \"Rate limit exceeded\", \"retryAfterSeconds\": %d, \"status\": 429}",
                    waitSeconds
            ));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
