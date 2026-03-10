package com.proxy.controller;

import com.proxy.dto.ApiKeyDto;
import com.proxy.service.ApiKeyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/keys")
@Slf4j
public class ApiKeyController {

    @Autowired
    private ApiKeyService apiKeyService;

    /** List all keys (no plaintext — prefixes only) */
    @GetMapping
    public ResponseEntity<List<ApiKeyDto.KeySummary>> listKeys() {
        return ResponseEntity.ok(apiKeyService.listKeys());
    }

    /** Create a new API key — full key shown once in response */
    @PostMapping
    public ResponseEntity<ApiKeyDto.CreateResponse> createKey(
            @RequestBody ApiKeyDto.CreateRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        ApiKeyDto.CreateResponse response = apiKeyService.createKey(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Revoke a key by ID */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> revokeKey(@PathVariable Long id) {
        boolean revoked = apiKeyService.revokeKey(id);
        if (!revoked) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
                "status", "revoked",
                "id", id,
                "message", "Key revoked successfully"
        ));
    }

    /** Generate a new key value (for testing/preview) */
    @GetMapping("/generate-preview")
    public ResponseEntity<Map<String, String>> previewKey() {
        return ResponseEntity.ok(Map.of(
                "preview", apiKeyService.generateRawKey(),
                "note", "This is not saved. POST to /admin/keys to create a real key."
        ));
    }
}
