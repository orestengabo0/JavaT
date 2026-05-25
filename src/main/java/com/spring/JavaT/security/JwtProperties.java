package com.spring.JavaT.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Typed binding for all {@code app.jwt.*} properties in {@code application.properties}.
 *
 * <p>Using a dedicated properties class instead of {@code @Value} fields means:
 * <ul>
 *   <li>All JWT config is in one place and easy to find.</li>
 *   <li>IDE auto-completion works in {@code application.properties}.</li>
 *   <li>Validation annotations can be added here if needed.</li>
 * </ul>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * Base64-encoded HMAC-SHA256 secret key.
     * Must be at least 256 bits (32 bytes) when decoded.
     */
    private String secret;

    /**
     * Access token validity in milliseconds.
     * Default: 86400000 (24 hours).
     */
    private long expirationMs;

    /**
     * Refresh token validity in milliseconds.
     * Default: 604800000 (7 days).
     */
    private long refreshExpirationMs;

    /**
     * Value placed in the JWT {@code iss} (issuer) claim.
     */
    private String issuer;
}
