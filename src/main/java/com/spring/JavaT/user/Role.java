package com.spring.JavaT.user;

/**
 * Application roles used for authorization.
 *
 * <p>Stored as strings in the database via {@code @Enumerated(EnumType.STRING)}.
 * Spring Security expects role names prefixed with {@code ROLE_} when using
 * {@code hasRole()} expressions — that prefix is added in {@link User#getAuthorities()}.
 */
public enum Role {

    /** Standard authenticated user. */
    USER,

    /** Moderator with elevated read/write access. */
    MODERATOR,

    /** Full administrative access. */
    ADMIN
}
