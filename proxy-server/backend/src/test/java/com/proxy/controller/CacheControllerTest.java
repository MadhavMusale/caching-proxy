package com.proxy.controller;

import com.proxy.service.ApiKeyService;
import com.proxy.service.ProxyService;
import com.proxy.model.CacheStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("CacheController Integration Tests")
class CacheControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProxyService proxyService;

    @MockBean
    private CacheManager cacheManager;

    @Autowired
    private ApiKeyService apiKeyService;

    private String getTestKey() {
        return "test-admin-key-12345"; // matches application-test.properties
    }

    @Test
    @DisplayName("Health endpoint should be public (no API key needed)")
    void healthEndpoint_isPublic() throws Exception {
        mockMvc.perform(get("/cache/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Stats endpoint requires API key")
    void statsEndpoint_requiresApiKey() throws Exception {
        mockMvc.perform(get("/cache/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Stats endpoint returns data with valid API key")
    void statsEndpoint_withValidKey_returnsStats() throws Exception {
        CacheStats mockStats = CacheStats.builder()
                .hitCount(10L).missCount(5L).totalRequests(15L)
                .hitRate(66.7).estimatedSize(100L).evictionCount(0L)
                .status("UP").recentRequests(List.of()).topEndpoints(List.of())
                .build();

        when(proxyService.getStats()).thenReturn(mockStats);

        mockMvc.perform(get("/cache/stats")
                        .header("X-API-Key", getTestKey()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hitCount").value(10))
                .andExpect(jsonPath("$.missCount").value(5))
                .andExpect(jsonPath("$.hitRate").value(66.7));
    }

    @Test
    @DisplayName("Clear cache returns success with valid key")
    void clearCache_withValidKey_succeeds() throws Exception {
        doNothing().when(proxyService).clearCache();

        mockMvc.perform(post("/cache/clear")
                        .header("X-API-Key", getTestKey()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("cleared"));

        verify(proxyService).clearCache();
    }

    @Test
    @DisplayName("Requests with wrong API key return 403")
    void wrongApiKey_returnsForbidden() throws Exception {
        mockMvc.perform(get("/cache/stats")
                        .header("X-API-Key", "wrong-key"))
                .andExpect(status().isForbidden());
    }
}
