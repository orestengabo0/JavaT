package com.spring.JavaT.notification;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Typed binding for {@code app.mail.*} properties.
 *
 * <p>Spring's own SMTP settings live under {@code spring.mail.*} and are
 * auto-configured. This class holds application-level mail settings:
 * the sender address, display name, base URL for links, and token expiry.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    /** The "From" address shown in outgoing emails. */
    private String from;

    /** The display name shown alongside the From address. */
    private String fromName;

    /**
     * Base URL of the application, used to build links in email bodies.
     * No trailing slash. Example: {@code http://localhost:8080}
     */
    private String baseUrl;
}
