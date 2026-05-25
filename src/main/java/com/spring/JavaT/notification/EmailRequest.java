package com.spring.JavaT.notification;

import lombok.Builder;
import lombok.Getter;

/**
 * Immutable value object carrying all data needed to send one email.
 *
 * <p>Used internally by {@link EmailService} — never exposed via the API.
 * Build instances with the Lombok builder:
 * <pre>
 * EmailRequest.builder()
 *     .to("user@example.com")
 *     .subject("Welcome")
 *     .body("Hello!")
 *     .html(true)
 *     .build();
 * </pre>
 */
@Getter
@Builder
public class EmailRequest {

    /** Recipient email address. */
    private final String to;

    /** Recipient display name (used in the "To" header). May be null. */
    private final String toName;

    /** Email subject line. */
    private final String subject;

    /** Email body — plain text or HTML depending on {@link #html}. */
    private final String body;

    /**
     * When {@code true} the body is sent as {@code text/html}.
     * When {@code false} (default) it is sent as {@code text/plain}.
     */
    @Builder.Default
    private final boolean html = false;
}
