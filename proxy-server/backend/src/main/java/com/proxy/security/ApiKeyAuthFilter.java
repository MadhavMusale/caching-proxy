package com.proxy.security;

import com.proxy.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Component
@Slf4j
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    static final String API_KEY_HEADER = "X-API-Key";

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/cache/health",
            "/actuator/health"
    );

    @Autowired
    private ApiKeyService apiKeyService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // Attach request ID to every log line
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("requestId", requestId);
        MDC.put("clientIp", getClientIp(request));
        response.addHeader("X-Request-ID", requestId);

        try {
            String path = request.getRequestURI();
            if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
                chain.doFilter(request, response);
                return;
            }

            String rawKey = request.getHeader(API_KEY_HEADER);
            if (rawKey == null || rawKey.isBlank()) {
                writeError(response, HttpStatus.UNAUTHORIZED, "Missing X-API-Key header");
                return;
            }

            var validKey = apiKeyService.validateKey(rawKey);
            if (validKey.isEmpty()) {
                log.warn("Invalid or expired API key from IP={}", getClientIp(request));
                writeError(response, HttpStatus.FORBIDDEN, "Invalid or expired API key");
                return;
            }

            // Add key context to logs
            MDC.put("keyId", String.valueOf(validKey.get().getId()));
            MDC.put("keyName", validKey.get().getName());

            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                String.format("{\"error\":\"%s\",\"status\":%d}", message, status.value()));
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }
}
