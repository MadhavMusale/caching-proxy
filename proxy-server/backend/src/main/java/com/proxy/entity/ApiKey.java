package com.proxy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "api_keys", indexes = {
    @Index(name = "idx_api_keys_key_hash", columnList = "key_hash", unique = true),
    @Index(name = "idx_api_keys_active", columnList = "active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "key_hash", nullable = false, unique = true, length = 64)
    private String keyHash; // SHA-256 hash — never store plaintext keys

    @Column(name = "key_prefix", length = 20)
    private String keyPrefix; // First 8 chars for display (e.g. "sk-abc123")

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "requests_count")
    @Builder.Default
    private long requestsCount = 0;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "expires_at")
    private Instant expiresAt; // null = never expires

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return active && !isExpired();
    }
}
