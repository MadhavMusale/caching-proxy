package com.proxy.service;

import com.proxy.validator.OriginValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@DisplayName("OriginValidator - SSRF Protection Tests")
class OriginValidatorTest {

    private OriginValidator validator;

    @BeforeEach
    void setup() {
        validator = new OriginValidator();
        ReflectionTestUtils.setField(validator, "allowedOriginsConfig", "*");
    }

    @ParameterizedTest(name = "Should allow valid origin: {0}")
    @ValueSource(strings = {
            "http://dummyjson.com",
            "https://jsonplaceholder.typicode.com",
            "https://api.github.com",
            "http://example.com",
    })
    void validate_legitimateOrigins_shouldPass(String origin) {
        var result = validator.validate(origin);
        assertThat(result.valid()).isTrue();
    }

    @ParameterizedTest(name = "Should block private IP: {0}")
    @ValueSource(strings = {
            "http://192.168.1.1",
            "http://10.0.0.1",
            "http://172.16.0.1",
            "http://127.0.0.1",
            "http://localhost",
            "http://0.0.0.0",
    })
    void validate_privateIPs_shouldBeBlocked(String origin) {
        var result = validator.validate(origin);
        assertThat(result.valid()).isFalse();
        assertThat(result.errorMessage()).containsIgnoringCase("not allowed");
    }

    @Test
    @DisplayName("Null origin should pass (uses default)")
    void validate_nullOrigin_shouldPass() {
        assertThat(validator.validate(null).valid()).isTrue();
    }

    @Test
    @DisplayName("Blank origin should pass (uses default)")
    void validate_blankOrigin_shouldPass() {
        assertThat(validator.validate("  ").valid()).isTrue();
    }

    @Test
    @DisplayName("Non-http scheme should be rejected")
    void validate_ftpScheme_shouldFail() {
        var result = validator.validate("ftp://example.com/data");
        assertThat(result.valid()).isFalse();
        assertThat(result.errorMessage()).containsIgnoringCase("http");
    }

    @Test
    @DisplayName("Malformed URL should be rejected")
    void validate_malformedUrl_shouldFail() {
        var result = validator.validate("not a url!!!");
        assertThat(result.valid()).isFalse();
    }

    @Test
    @DisplayName("Allowlist should block unlisted domains")
    void validate_withAllowlist_blocksUnlistedDomain() {
        ReflectionTestUtils.setField(validator, "allowedOriginsConfig", "dummyjson.com,jsonplaceholder.typicode.com");

        assertThat(validator.validate("https://dummyjson.com").valid()).isTrue();
        assertThat(validator.validate("https://evil.com").valid()).isFalse();
    }

    @Test
    @DisplayName("Allowlist should allow subdomains")
    void validate_withAllowlist_allowsSubdomains() {
        ReflectionTestUtils.setField(validator, "allowedOriginsConfig", "example.com");

        assertThat(validator.validate("https://api.example.com").valid()).isTrue();
        assertThat(validator.validate("https://example.com").valid()).isTrue();
    }
}
