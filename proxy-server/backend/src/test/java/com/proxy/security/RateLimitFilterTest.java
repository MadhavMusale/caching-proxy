package com.proxy.security;

import com.proxy.filter.RateLimitFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("RateLimitFilter Unit Tests")
class RateLimitFilterTest {

    private RateLimitFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setup() {
        filter = new RateLimitFilter();
        ReflectionTestUtils.setField(filter, "requestsPerMinute", 5);
        ReflectionTestUtils.setField(filter, "burstCapacity", 3);
        chain = mock(FilterChain.class);
    }

    @Test
    @DisplayName("Requests within limit should pass through")
    void withinLimit_shouldPassThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("1.2.3.4");
        request.setRequestURI("/proxy/products");

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isNotEqualTo(429);
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Rate limit headers should be set on all responses")
    void rateLimitHeaders_shouldBePresent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("2.3.4.5");
        request.setRequestURI("/proxy/test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("5");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isNotNull();
    }

    @Test
    @DisplayName("Health check path should bypass rate limiting")
    void healthPath_bypassesRateLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("3.4.5.6");
        request.setRequestURI("/cache/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getHeader("X-RateLimit-Limit")).isNull();
    }

    @Test
    @DisplayName("Exceeding burst limit should return 429")
    void exceedingBurst_returns429() throws Exception {
        String ip = "4.5.6.7";
        // Exhaust the bucket
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setRemoteAddr(ip);
            req.setRequestURI("/proxy/test");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilterInternal(req, res, chain);
        }

        // This one should be rate limited
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr(ip);
        req.setRequestURI("/proxy/test");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilterInternal(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getHeader("Retry-After")).isNotNull();
    }
}
