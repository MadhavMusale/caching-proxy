package com.proxy.service;

import com.proxy.dto.ApiKeyDto;
import com.proxy.entity.ApiKey;
import com.proxy.repository.ApiKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiKeyService Unit Tests")
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository repository;

    @InjectMocks
    private ApiKeyService service;

    private ApiKey activeKey;
    private String rawKey;

    @BeforeEach
    void setup() {
        rawKey = "cpx-testkey12345678901234567890";
        activeKey = ApiKey.builder()
                .id(1L)
                .name("Test Key")
                .keyHash(service.hash(rawKey))
                .keyPrefix("cpx-testkey")
                .active(true)
                .requestsCount(0)
                .build();
    }

    @Test
    @DisplayName("Valid active key should authenticate successfully")
    void validateKey_validKey_returnsKey() {
        when(repository.findByKeyHashAndActiveTrue(service.hash(rawKey)))
                .thenReturn(Optional.of(activeKey));

        var result = service.validateKey(rawKey);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Test Key");
        verify(repository).incrementUsage(eq(1L), any(Instant.class));
    }

    @Test
    @DisplayName("Invalid key should return empty")
    void validateKey_invalidKey_returnsEmpty() {
        when(repository.findByKeyHashAndActiveTrue(anyString()))
                .thenReturn(Optional.empty());

        var result = service.validateKey("wrong-key");

        assertThat(result).isEmpty();
        verify(repository, never()).incrementUsage(any(), any());
    }

    @Test
    @DisplayName("Null key should return empty without DB call")
    void validateKey_nullKey_returnsEmpty() {
        var result = service.validateKey(null);

        assertThat(result).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Expired key should not authenticate")
    void validateKey_expiredKey_returnsEmpty() {
        ApiKey expiredKey = ApiKey.builder()
                .id(2L)
                .name("Expired Key")
                .keyHash(service.hash(rawKey))
                .keyPrefix("cpx-testkey")
                .active(true)
                .expiresAt(Instant.now().minusSeconds(3600))
                .build();

        when(repository.findByKeyHashAndActiveTrue(service.hash(rawKey)))
                .thenReturn(Optional.of(expiredKey));

        var result = service.validateKey(rawKey);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Created key should have correct structure")
    void createKey_returnsFullKeyOnce() {
        when(repository.save(any(ApiKey.class))).thenAnswer(inv -> {
            ApiKey k = inv.getArgument(0);
            k = ApiKey.builder()
                    .id(99L).name(k.getName()).keyHash(k.getKeyHash())
                    .keyPrefix(k.getKeyPrefix()).active(true).build();
            return k;
        });

        var request = new ApiKeyDto.CreateRequest("My Service", null);
        var response = service.createKey(request);

        assertThat(response.getKey()).startsWith("cpx-");
        assertThat(response.getKey()).hasSizeGreaterThan(20);
        assertThat(response.getName()).isEqualTo("My Service");
        assertThat(response.getMessage()).contains("not be shown again");
    }

    @Test
    @DisplayName("Key hashing should be deterministic")
    void hash_deterministicOutput() {
        String hash1 = service.hash("test-key");
        String hash2 = service.hash("test-key");
        String hash3 = service.hash("different-key");

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).isNotEqualTo(hash3);
        assertThat(hash1).hasSize(64); // SHA-256 hex
    }

    @Test
    @DisplayName("Generated keys should be unique and correctly prefixed")
    void generateRawKey_uniqueAndPrefixed() {
        String key1 = service.generateRawKey();
        String key2 = service.generateRawKey();

        assertThat(key1).startsWith("cpx-");
        assertThat(key2).startsWith("cpx-");
        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    @DisplayName("Revoke should call repository and return true for existing key")
    void revokeKey_existingKey_returnsTrue() {
        when(repository.existsById(1L)).thenReturn(true);

        boolean result = service.revokeKey(1L);

        assertThat(result).isTrue();
        verify(repository).revokeById(1L);
    }

    @Test
    @DisplayName("Revoke non-existent key should return false")
    void revokeKey_nonExistentKey_returnsFalse() {
        when(repository.existsById(999L)).thenReturn(false);

        boolean result = service.revokeKey(999L);

        assertThat(result).isFalse();
        verify(repository, never()).revokeById(any());
    }

    @Test
    @DisplayName("List keys should return summaries without plaintext keys")
    void listKeys_returnsSummariesOnly() {
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(activeKey));

        var summaries = service.listKeys();

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).getKeyPrefix()).endsWith("…");
        // Verify no full key is exposed
        assertThat(summaries.get(0).toString()).doesNotContain(rawKey);
    }
}
