package com.spring.JavaT.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested resource does not exist.
 *
 * <p>Maps to HTTP 404 Not Found.
 *
 * <p>Usage:
 * <pre>
 * // With resource name and identifier
 * throw new ResourceNotFoundException("User", "id", 42L);
 *
 * // With a plain message
 * throw new ResourceNotFoundException("User not found");
 * </pre>
 */
public class ResourceNotFoundException extends BusinessException {

    private static final String ERROR_CODE = "RESOURCE_NOT_FOUND";

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, ERROR_CODE);
    }

    /**
     * Convenience constructor that builds a standard message:
     * "{resourceName} not found with {fieldName}: '{fieldValue}'"
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(
            "%s not found with %s: '%s'".formatted(resourceName, fieldName, fieldValue),
            HttpStatus.NOT_FOUND,
            ERROR_CODE
        );
    }
}
