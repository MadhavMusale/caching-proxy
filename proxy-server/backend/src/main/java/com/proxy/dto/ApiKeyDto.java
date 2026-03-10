package com.proxy.dto;

import lombok.*;
import java.time.Instant;

public class ApiKeyDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private String name;
        private Instant expiresAt; // optional
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateResponse {
        private Long id;
        private String name;
        private String key;        // full key — shown ONCE on creation
        private String keyPrefix;
        private Instant createdAt;
        private Instant expiresAt;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeySummary {
        private Long id;
        private String name;
        private String keyPrefix;  // e.g. "sk-abc123" — never full key
        private boolean active;
        private long requestsCount;
        private Instant lastUsedAt;
        private Instant createdAt;
        private Instant expiresAt;
        private boolean expired;
    }
}
