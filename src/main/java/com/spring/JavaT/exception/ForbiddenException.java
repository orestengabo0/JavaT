package com.spring.JavaT.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an authenticated user attempts an action they are not permitted to perform.
 *
 * <p>Maps to HTTP 403 Forbidden.
 *
 * <p>Usage:
 * <pre>
 * throw new ForbiddenException("You do not have permission to delete this resource");
 * </pre>
 */
public class ForbiddenException extends BusinessException {

    private static final String ERROR_CODE = "FORBIDDEN";

    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN, ERROR_CODE);
    }
}
