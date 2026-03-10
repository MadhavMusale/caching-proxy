package com.proxy.controller;

import com.proxy.model.CachedResponse;
import com.proxy.model.ProxyRequest;
import com.proxy.service.ProxyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/proxy")
@Slf4j
public class ProxyController {

    @Autowired
    private ProxyService proxyService;

    @RequestMapping(value = "/**", method = {
            RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
            RequestMethod.DELETE, RequestMethod.PATCH
    })
    public ResponseEntity<String> proxyRequest(
            HttpServletRequest request,
            @RequestParam(required = false) String origin,
            @RequestBody(required = false) String body) throws IOException {

        String fullPath = request.getRequestURI().replaceFirst("/proxy", "");
        if (fullPath.isEmpty()) fullPath = "/";

        String queryString = request.getQueryString();
        // Remove 'origin' param from query string if present
        if (queryString != null) {
            queryString = java.util.Arrays.stream(queryString.split("&"))
                    .filter(p -> !p.startsWith("origin="))
                    .collect(Collectors.joining("&"));
            if (queryString.isEmpty()) queryString = null;
        }

        ProxyRequest proxyRequest = ProxyRequest.builder()
                .method(request.getMethod())
                .path(fullPath)
                .queryString(queryString)
                .body(body)
                .origin(origin)
                .build();

        log.info("Proxying {} {} -> origin: {}", request.getMethod(), fullPath, origin);

        CachedResponse cached = proxyService.proxy(proxyRequest);

        HttpHeaders headers = new HttpHeaders();
        if (cached.getHeaders() != null) {
            cached.getHeaders().forEach((k, v) -> {
                if (!k.equalsIgnoreCase("Transfer-Encoding") &&
                    !k.equalsIgnoreCase("Content-Encoding")) {
                    headers.add(k, v);
                }
            });
        }

        // Add proxy metadata headers
        boolean isHit = cached.getResponseTimeMs() == 0 ||
                (cached.getCachedAt() != null &&
                 System.currentTimeMillis() - cached.getCachedAt().toEpochMilli() > 100);
        headers.set("X-Cache", isHit ? "HIT" : "MISS");
        headers.set("X-Proxy", "CachingProxy/1.0");
        headers.set("X-Cache-Key", cached.getCacheKey());
        headers.set("X-Response-Time", cached.getResponseTimeMs() + "ms");
        headers.set("Content-Type", "application/json");

        return ResponseEntity.status(HttpStatus.valueOf(cached.getStatusCode()))
                .headers(headers)
                .body(cached.getBody());
    }
}
