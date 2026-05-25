package com.spring.JavaT.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an operation would create a duplicate of a resource that must be unique.
 *
 * <p>Maps to HTTP 409 Conflict.
 *
 * <p>Usage:
 * <pre>
 * // With resource name and field
 * throw new DuplicateResourceException("User", "email", "john@example.com");
 *
 * // With a plain message
 * throw new DuplicateResourceException("A user with this email already exists");
 * </pre>
 */
public class DuplicateResourceException extends BusinessException {

    private static final String ERROR_CODE = "DUPLICATE_RESOURCE";

    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT, ERROR_CODE);
    }

    /**
     * Convenience constructor that builds a standard message:
     * "{resourceName} already exists with {fieldName}: '{fieldValue}'"
     */
    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(
            "%s already exists with %s: '%s'".formatted(resourceName, fieldName, fieldValue),
            HttpStatus.CONFLICT,
            ERROR_CODE
        );
    }
}
