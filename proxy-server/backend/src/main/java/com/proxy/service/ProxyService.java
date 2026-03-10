package com.proxy.service;

import com.proxy.config.RedisConfig;
import com.proxy.exception.OriginNotAllowedException;
import com.proxy.model.CacheStats;
import com.proxy.model.CachedResponse;
import com.proxy.model.ProxyRequest;
import com.proxy.validator.OriginValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProxyService {

    @Value("${proxy.origin:http://dummyjson.com}")
    private String defaultOrigin;

    @Value("${proxy.timeout-seconds:30}")
    private int timeoutSeconds;

    @Autowired
    private WebClient webClient;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private OriginValidator originValidator;

    // Local dev cache (replaces Redis)

    private final Map<String, CachedResponse> localCache = new ConcurrentHashMap<>();
    private final AtomicLong totalHits = new AtomicLong(0);
    private final AtomicLong totalMisses = new AtomicLong(0);
    private final Deque<CacheStats.RecentRequest> recentRequests = new ConcurrentLinkedDeque<>();
    private final Map<String, AtomicLong> endpointCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> endpointHits = new ConcurrentHashMap<>();

    private static final int MAX_RECENT = 50;

    public String buildCacheKey(String method, String origin, String path, String queryString) {
        String key = method.toUpperCase() + ":" + origin + path;
        if (queryString != null && !queryString.isEmpty()) {
            key += "?" + queryString;
        }
        return key;
    }

    public CachedResponse proxy(ProxyRequest request) {
        String origin = (request.getOrigin() != null && !request.getOrigin().isBlank())
                ? request.getOrigin() : defaultOrigin;

        // Validate origin before proceeding
        OriginValidator.ValidationResult validation = originValidator.validate(origin);
        if (!validation.valid()) {
            throw new OriginNotAllowedException(validation.errorMessage());
        }

        String cacheKey = buildCacheKey(request.getMethod(), origin, request.getPath(),
                request.getQueryString());

        endpointCounts.computeIfAbsent(request.getPath(), k -> new AtomicLong(0)).incrementAndGet();

        // Local dev cache (no Redis)
        CachedResponse cached = localCache.get(cacheKey);
        if (cached != null) {
            log.info("CACHE HIT: {}", cacheKey);
            totalHits.incrementAndGet();
            endpointHits.computeIfAbsent(request.getPath(), k -> new AtomicLong(0)).incrementAndGet();
            recordRequest(request, true, 0, origin);
            return cached;
        }

        log.info("CACHE MISS: {} - forwarding to {}", cacheKey, origin);
        totalMisses.incrementAndGet();

        long start = System.currentTimeMillis();
        CachedResponse response = forwardRequest(request, origin, cacheKey, start);

        // Local dev cache (no Redis)
        if (response.getStatusCode() < 400) {
            localCache.put(cacheKey, response);
        }

        recordRequest(request, false, response.getResponseTimeMs(), origin);
        return response;
    }

    private CachedResponse forwardRequest(ProxyRequest request, String origin, String cacheKey, long start) {
        try {
            String url = origin + request.getPath();
            if (request.getQueryString() != null && !request.getQueryString().isBlank()) {
                url += "?" + request.getQueryString();
            }

            WebClient.RequestHeadersSpec<?> req = switch (request.getMethod().toUpperCase()) {
                case "POST" -> webClient.post().uri(url)
                        .bodyValue(request.getBody() != null ? request.getBody() : "");
                case "PUT" -> webClient.put().uri(url)
                        .bodyValue(request.getBody() != null ? request.getBody() : "");
                case "DELETE" -> webClient.delete().uri(url);
                default -> webClient.get().uri(url);
            };

            var responseEntity = req
                    .header("Accept", "application/json")
                    .retrieve()
                    .toEntity(String.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            long elapsed = System.currentTimeMillis() - start;

            Map<String, String> headers = new HashMap<>();
            if (responseEntity != null && responseEntity.getHeaders() != null) {
                responseEntity.getHeaders().forEach((k, v) -> headers.put(k, String.join(",", v)));
            }

            return CachedResponse.builder()
                    .body(responseEntity != null ? responseEntity.getBody() : "")
                    .statusCode(responseEntity != null ? responseEntity.getStatusCode().value() : 200)
                    .headers(headers)
                    .cacheKey(cacheKey)
                    .cachedAt(Instant.now())
                    .responseTimeMs(elapsed)
                    .origin(origin)
                    .path(request.getPath())
                    .method(request.getMethod())
                    .build();

        } catch (WebClientResponseException ex) {
            long elapsed = System.currentTimeMillis() - start;
            return CachedResponse.builder()
                    .body(ex.getResponseBodyAsString())
                    .statusCode(ex.getStatusCode().value())
                    .headers(new HashMap<>())
                    .cacheKey(cacheKey)
                    .cachedAt(Instant.now())
                    .responseTimeMs(elapsed)
                    .origin(origin)
                    .path(request.getPath())
                    .method(request.getMethod())
                    .build();
        } catch (Exception ex) {
            log.error("Error forwarding request: {}", ex.getMessage());
            long elapsed = System.currentTimeMillis() - start;
            return CachedResponse.builder()
                    .body("{\"error\": \"Upstream request failed\"}")
                    .statusCode(502)
                    .headers(new HashMap<>())
                    .cacheKey(cacheKey)
                    .cachedAt(Instant.now())
                    .responseTimeMs(elapsed)
                    .origin(origin)
                    .path(request.getPath())
                    .method(request.getMethod())
                    .build();
        }
    }

    private void recordRequest(ProxyRequest request, boolean hit, long responseTime, String resolvedOrigin) {
        CacheStats.RecentRequest rec = CacheStats.RecentRequest.builder()
                .method(request.getMethod())
                .path(request.getPath())
                .origin(resolvedOrigin)
                .cacheHit(hit)
                .responseTimeMs(responseTime)
                .timestamp(DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
                .build();

        recentRequests.addFirst(rec);
        while (recentRequests.size() > MAX_RECENT) {
            recentRequests.pollLast();
        }
    }

    public CacheStats getStats() {
        long hits = totalHits.get();
        long misses = totalMisses.get();
        long total = hits + misses;
        double hitRate = total > 0 ? (double) hits / total * 100 : 0;

        List<CacheStats.TopEndpoint> topEndpoints = endpointCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().get(), a.getValue().get()))
                .limit(10)
                .map(e -> CacheStats.TopEndpoint.builder()
                        .path(e.getKey())
                        .requestCount(e.getValue().get())
                        .cacheHits(endpointHits.getOrDefault(e.getKey(), new AtomicLong(0)).get())
                        .build())
                .collect(Collectors.toList());

        return CacheStats.builder()
                .hitCount(hits)
                .missCount(misses)
                .totalRequests(total)
                .hitRate(hitRate)
                .estimatedSize(endpointCounts.size())
                .evictionCount(0L)
                .status("UP")
                .recentRequests(new ArrayList<>(recentRequests))
                .topEndpoints(topEndpoints)
                .build();
    }

    public void clearCache() {
        org.springframework.cache.Cache springCache = cacheManager.getCache(RedisConfig.PROXY_CACHE);
        if (springCache != null) {
            springCache.clear();
        }
        log.info("Cache cleared");
    }

    public String getDefaultOrigin() { return defaultOrigin; }

    public void setDefaultOrigin(String origin) {
        OriginValidator.ValidationResult v = originValidator.validate(origin);
        if (!v.valid()) throw new OriginNotAllowedException(v.errorMessage());
        this.defaultOrigin = origin;
        log.info("Origin updated to: {}", origin);
    }
}
