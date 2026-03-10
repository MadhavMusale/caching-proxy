package com.proxy.repository;

import com.proxy.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByKeyHashAndActiveTrue(String keyHash);

    List<ApiKey> findAllByOrderByCreatedAtDesc();

    List<ApiKey> findByActiveTrue();

    @Modifying
    @Transactional
    @Query("UPDATE ApiKey k SET k.requestsCount = k.requestsCount + 1, k.lastUsedAt = :now WHERE k.id = :id")
    void incrementUsage(Long id, Instant now);

    @Modifying
    @Transactional
    @Query("UPDATE ApiKey k SET k.active = false WHERE k.id = :id")
    void revokeById(Long id);
}
