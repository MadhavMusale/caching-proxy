package com.proxy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CachedResponse implements Serializable {
    private String body;
    private int statusCode;
    private Map<String, String> headers;
    private String cacheKey;
    private Instant cachedAt;
    private long responseTimeMs;
    private String origin;
    private String path;
    private String method;
}
