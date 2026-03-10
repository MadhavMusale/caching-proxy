package com.proxy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheStats {
    private long hitCount;
    private long missCount;
    private long totalRequests;
    private double hitRate;
    private long estimatedSize;
    private long evictionCount;
    private double avgLoadPenalty;
    private String status;
    private List<RecentRequest> recentRequests;
    private List<TopEndpoint> topEndpoints;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentRequest {
        private String method;
        private String path;
        private String origin;
        private boolean cacheHit;
        private long responseTimeMs;
        private String timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopEndpoint {
        private String path;
        private long requestCount;
        private long cacheHits;
    }
}
