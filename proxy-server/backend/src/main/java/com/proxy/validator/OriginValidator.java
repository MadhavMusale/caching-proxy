package com.proxy.validator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class OriginValidator {

    // Private/internal IP ranges that must never be proxied (SSRF protection)
    private static final Set<String> BLOCKED_PREFIXES = Set.of(
            "10.", "192.168.", "172.16.", "172.17.", "172.18.", "172.19.",
            "172.20.", "172.21.", "172.22.", "172.23.", "172.24.", "172.25.",
            "172.26.", "172.27.", "172.28.", "172.29.", "172.30.", "172.31.",
            "127.", "0.", "169.254.", "::1", "fc", "fd"
    );

    @Value("${proxy.allowed-origins:*}")
    private String allowedOriginsConfig;

    /**
     * Validates an origin URL.
     * - Blocks private/internal IPs (SSRF protection)
     * - Checks against allowlist if configured
     * - Ensures URL is well-formed
     */
    public ValidationResult validate(String originUrl) {
        if (originUrl == null || originUrl.isBlank()) {
            return ValidationResult.ok(); // Will use default origin
        }

        // Must be a valid URI
        URI uri;
        try {
            uri = URI.create(originUrl);
        } catch (Exception e) {
            return ValidationResult.fail("Invalid URL format: " + originUrl);
        }

        // Must have http or https scheme
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
            return ValidationResult.fail("Origin must use http or https scheme");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return ValidationResult.fail("Origin URL must have a valid host");
        }

        // SSRF protection — block private IPs
        if (isPrivateOrInternalHost(host)) {
            log.warn("SSRF attempt blocked for host: {}", host);
            return ValidationResult.fail("Origin host is not allowed (private/internal address)");
        }

        // Allowlist check (if configured)
        if (!allowedOriginsConfig.equals("*")) {
            Set<String> allowed = Arrays.stream(allowedOriginsConfig.split(","))
                    .map(String::trim)
                    .collect(Collectors.toSet());

            boolean permitted = allowed.stream().anyMatch(a ->
                    host.equals(a) || host.endsWith("." + a));

            if (!permitted) {
                log.warn("Origin not in allowlist: {}", host);
                return ValidationResult.fail(
                        "Origin '" + host + "' is not in the permitted origins list");
            }
        }

        return ValidationResult.ok();
    }

    private boolean isPrivateOrInternalHost(String host) {
        // Check by prefix
        for (String prefix : BLOCKED_PREFIXES) {
            if (host.startsWith(prefix)) return true;
        }

        // Also resolve DNS to catch aliased private IPs
        try {
            InetAddress addr = InetAddress.getByName(host);
            String resolved = addr.getHostAddress();
            for (String prefix : BLOCKED_PREFIXES) {
                if (resolved.startsWith(prefix)) return true;
            }
            return addr.isLoopbackAddress() || addr.isSiteLocalAddress()
                    || addr.isLinkLocalAddress() || addr.isAnyLocalAddress();
        } catch (Exception e) {
            // If we can't resolve, be safe and allow (avoids blocking legit external domains)
            return false;
        }
    }

    public record ValidationResult(boolean valid, String errorMessage) {
        static ValidationResult ok() { return new ValidationResult(true, null); }
        static ValidationResult fail(String msg) { return new ValidationResult(false, msg); }
    }
}
