package com.spring.JavaT.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request lacks valid authentication credentials.
 *
 * <p>Maps to HTTP 401 Unauthorized.
 *
 * <p>This is for application-level auth checks (e.g. token expired, bad credentials).
 * Spring Security's own 401 responses are handled separately via
 * {@code AuthenticationEntryPoint} in the security config.
 *
 * <p>Usage:
 * <pre>
 * throw new UnauthorizedException("Invalid or expired token");
 * </pre>
 */
public class UnauthorizedException extends BusinessException {

    private static final String ERROR_CODE = "UNAUTHORIZED";

    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, ERROR_CODE);
    }
}
