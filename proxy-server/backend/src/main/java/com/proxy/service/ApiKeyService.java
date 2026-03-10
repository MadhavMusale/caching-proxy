package com.proxy.service;

import com.proxy.dto.ApiKeyDto;
import com.proxy.entity.ApiKey;
import com.proxy.repository.ApiKeyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ApiKeyService {

    private static final String KEY_PREFIX = "cpx-";
    private static final int KEY_RANDOM_BYTES = 32;

    @Value("${proxy.admin-key:#{null}}")
    private String adminKeyFromEnv;

    @Autowired
    private ApiKeyRepository repository;

    /**
     * On startup: if ADMIN_API_KEY env var is set and no keys exist, seed it.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void seedInitialKey() {
        if (repository.count() == 0) {
            String rawKey = adminKeyFromEnv != null && !adminKeyFromEnv.isBlank()
                    ? adminKeyFromEnv
                    : generateRawKey();

            ApiKey key = ApiKey.builder()
                    .name("Default Admin Key")
                    .keyHash(hash(rawKey))
                    .keyPrefix(rawKey.substring(0, Math.min(12, rawKey.length())))
                    .active(true)
                    .build();
            repository.save(key);

            if (adminKeyFromEnv == null || adminKeyFromEnv.isBlank()) {
                log.warn("╔══════════════════════════════════════════════════════╗");
                log.warn("║  AUTO-GENERATED ADMIN KEY (save this — shown once!) ║");
                log.warn("║  {}  ║", rawKey);
                log.warn("╚══════════════════════════════════════════════════════╝");
            } else {
                log.info("Seeded admin key from ADMIN_API_KEY env var.");
            }
        }
    }

    public ApiKeyDto.CreateResponse createKey(ApiKeyDto.CreateRequest request) {
        String raw = generateRawKey();
        String hash = hash(raw);
        String prefix = raw.substring(0, 12);

        ApiKey key = ApiKey.builder()
                .name(request.getName() != null ? request.getName() : "Unnamed Key")
                .keyHash(hash)
                .keyPrefix(prefix)
                .active(true)
                .expiresAt(request.getExpiresAt())
                .build();

        ApiKey saved = repository.save(key);
        log.info("Created API key '{}' id={}", saved.getName(), saved.getId());

        return ApiKeyDto.CreateResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .key(raw) // Full key shown ONCE — not stored
                .keyPrefix(prefix)
                .createdAt(saved.getCreatedAt())
                .expiresAt(saved.getExpiresAt())
                .message("Store this key securely — it will not be shown again.")
                .build();
    }

    public Optional<ApiKey> validateKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) return Optional.empty();
        String hash = hash(rawKey.trim());
        Optional<ApiKey> key = repository.findByKeyHashAndActiveTrue(hash);

        key.ifPresent(k -> {
            if (k.isExpired()) {
                log.warn("Expired key used: id={}", k.getId());
                return;
            }
            // Async usage tracking
            repository.incrementUsage(k.getId(), Instant.now());
        });

        return key.filter(ApiKey::isValid);
    }

    public List<ApiKeyDto.KeySummary> listKeys() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    public boolean revokeKey(Long id) {
        if (!repository.existsById(id)) return false;
        repository.revokeById(id);
        log.info("Revoked API key id={}", id);
        return true;
    }

    private ApiKeyDto.KeySummary toSummary(ApiKey key) {
        return ApiKeyDto.KeySummary.builder()
                .id(key.getId())
                .name(key.getName())
                .keyPrefix(key.getKeyPrefix() + "…")
                .active(key.isActive())
                .requestsCount(key.getRequestsCount())
                .lastUsedAt(key.getLastUsedAt())
                .createdAt(key.getCreatedAt())
                .expiresAt(key.getExpiresAt())
                .expired(key.isExpired())
                .build();
    }

    public String generateRawKey() {
        SecureRandom rng = new SecureRandom();
        byte[] bytes = new byte[KEY_RANDOM_BYTES];
        rng.nextBytes(bytes);
        return KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }
}
