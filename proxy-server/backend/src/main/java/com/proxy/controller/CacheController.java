package com.proxy.controller;

import com.proxy.model.CacheStats;
import com.proxy.service.ProxyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/cache")
@Slf4j
public class CacheController {

    @Autowired
    private ProxyService proxyService;

    @GetMapping("/stats")
    public ResponseEntity<CacheStats> getStats() {
        return ResponseEntity.ok(proxyService.getStats());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        CacheStats stats = proxyService.getStats();
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "origin", proxyService.getDefaultOrigin(),
                "cacheSize", stats.getEstimatedSize(),
                "hitRate", String.format("%.1f%%", stats.getHitRate()),
                "totalRequests", stats.getTotalRequests()
        ));
    }

    @PostMapping("/clear")
    public ResponseEntity<Map<String, String>> clearCache() {
        proxyService.clearCache();
        return ResponseEntity.ok(Map.of(
                "status", "cleared",
                "message", "Cache cleared successfully"
        ));
    }

    @PutMapping("/origin")
    public ResponseEntity<Map<String, String>> updateOrigin(@RequestBody Map<String, String> body) {
        String newOrigin = body.get("origin");
        if (newOrigin == null || newOrigin.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "origin is required"));
        }
        proxyService.setDefaultOrigin(newOrigin);
        return ResponseEntity.ok(Map.of(
                "status", "updated",
                "origin", newOrigin
        ));
    }

    @GetMapping("/origin")
    public ResponseEntity<Map<String, String>> getOrigin() {
        return ResponseEntity.ok(Map.of("origin", proxyService.getDefaultOrigin()));
    }
}
