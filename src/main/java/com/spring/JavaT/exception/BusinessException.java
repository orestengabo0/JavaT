package com.spring.JavaT.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for all application-level business exceptions.
 *
 * <p>Subclass this when you need a domain-specific exception that maps to a
 * particular HTTP status. The {@link GlobalExceptionHandler} catches this base
 * type as a fallback for any subclass that isn't handled more specifically.
 *
 * <p>Example:
 * <pre>
 * throw new BusinessException("Account is suspended", HttpStatus.FORBIDDEN);
 * </pre>
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status    = status;
        this.errorCode = null;
    }

    public BusinessException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status    = status;
        this.errorCode = errorCode;
    }

    public BusinessException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status    = status;
        this.errorCode = null;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
