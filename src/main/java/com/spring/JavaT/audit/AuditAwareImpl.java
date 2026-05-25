package com.spring.JavaT.audit;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Provides the current authenticated principal's identifier to Spring Data JPA's
 * auditing mechanism.
 *
 * <p>This is used to populate the {@code createdBy} and {@code updatedBy} fields
 * on every entity that extends {@link com.spring.JavaT.common.BaseEntity}.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>If a fully authenticated user is present in the {@link SecurityContextHolder},
 *       their username is returned.</li>
 *   <li>If the request is unauthenticated (e.g. the registration endpoint before a
 *       user exists), {@code "system"} is returned as a safe fallback so that
 *       {@code createdBy} is never {@code null}.</li>
 * </ol>
 *
 * <p>To store the user's ID instead of their username, replace
 * {@code authentication.getName()} with a cast to your {@code UserDetails}
 * implementation and call the appropriate ID getter.
 */
public class AuditAwareImpl implements AuditorAware<String> {

    private static final String SYSTEM_USER = "system";

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.of(SYSTEM_USER);
        }

        return Optional.of(authentication.getName());
    }
}
