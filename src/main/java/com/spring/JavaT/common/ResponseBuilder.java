package com.spring.JavaT.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Utility class for building {@link ResponseEntity} objects wrapping {@link ApiResponse}.
 *
 * <p>Controllers should use this instead of constructing {@link ApiResponse} directly,
 * so the request path is always populated and HTTP status codes are consistent.
 *
 * <p>Usage examples:
 * <pre>
 * // 200 with data
 * return ResponseBuilder.ok(userDto, request);
 *
 * // 201 with data
 * return ResponseBuilder.created(userDto, "User registered successfully", request);
 *
 * // 204 no content
 * return ResponseBuilder.noContent();
 *
 * // 400 with validation errors
 * return ResponseBuilder.badRequest("Validation failed", errors, request);
 * </pre>
 */
public final class ResponseBuilder {

    private ResponseBuilder() {
        // utility class — no instantiation
    }

    // -------------------------------------------------------------------------
    // 2xx Success
    // -------------------------------------------------------------------------

    /** 200 OK with a payload. */
    public static <T> ResponseEntity<ApiResponse<T>> ok(T data, HttpServletRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<T>builder()
                        .success(true)
                        .message("Operation successful")
                        .data(data)
                        .path(request.getRequestURI())
                        .build()
        );
    }

    /** 200 OK with a payload and a custom message. */
    public static <T> ResponseEntity<ApiResponse<T>> ok(T data, String message, HttpServletRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<T>builder()
                        .success(true)
                        .message(message)
                        .data(data)
                        .path(request.getRequestURI())
                        .build()
        );
    }

    /** 200 OK with no payload (e.g. a simple confirmation message). */
    public static <T> ResponseEntity<ApiResponse<T>> ok(String message, HttpServletRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<T>builder()
                        .success(true)
                        .message(message)
                        .path(request.getRequestURI())
                        .build()
        );
    }

    /** 201 Created with a payload. */
    public static <T> ResponseEntity<ApiResponse<T>> created(T data, String message, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.<T>builder()
                                .success(true)
                                .message(message)
                                .data(data)
                                .path(request.getRequestURI())
                                .build()
                );
    }

    /** 204 No Content — no body. */
    public static <T> ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Paginated responses
    // -------------------------------------------------------------------------

    /**
     * 200 OK with a paginated payload built directly from a Spring {@link Page}.
     *
     * <pre>
     *     Page&lt;UserDto&gt; page = userService.findAll(pageable);
     *     return ResponseBuilder.ok(page, "Users retrieved successfully", request);
     * </pre>
     */
    public static <T> ResponseEntity<ApiResponse<PagedResponse<T>>> ok(
            Page<T> page, String message, HttpServletRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<PagedResponse<T>>builder()
                        .success(true)
                        .message(message)
                        .data(PagedResponse.of(page))
                        .path(request.getRequestURI())
                        .build()
        );
    }

    /**
     * 200 OK with a paginated payload where content has already been mapped to a DTO.
     *
     * <pre>
     *     Page&lt;User&gt; page = userRepository.findAll(pageable);
     *     List&lt;UserDto&gt; dtos = page.getContent().stream().map(mapper::toDto).toList();
     *     return ResponseBuilder.ok(dtos, page, "Users retrieved successfully", request);
     * </pre>
     */
    public static <T, S> ResponseEntity<ApiResponse<PagedResponse<T>>> ok(
            List<T> mappedContent, Page<S> sourcePage, String message, HttpServletRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<PagedResponse<T>>builder()
                        .success(true)
                        .message(message)
                        .data(PagedResponse.of(mappedContent, sourcePage))
                        .path(request.getRequestURI())
                        .build()
        );
    }

    // -------------------------------------------------------------------------
    // 4xx Client Errors
    // -------------------------------------------------------------------------

    /** 400 Bad Request with a list of errors. */
    public static <T> ResponseEntity<ApiResponse<T>> badRequest(
            String message, List<ApiError> errors, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        ApiResponse.<T>builder()
                                .success(false)
                                .message(message)
                                .errors(errors)
                                .path(request.getRequestURI())
                                .build()
                );
    }

    /** 400 Bad Request with a single message. */
    public static <T> ResponseEntity<ApiResponse<T>> badRequest(String message, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        ApiResponse.<T>builder()
                                .success(false)
                                .message(message)
                                .path(request.getRequestURI())
                                .build()
                );
    }

    /** 401 Unauthorized. */
    public static <T> ResponseEntity<ApiResponse<T>> unauthorized(String message, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(
                        ApiResponse.<T>builder()
                                .success(false)
                                .message(message)
                                .path(request.getRequestURI())
                                .build()
                );
    }

    /** 403 Forbidden. */
    public static <T> ResponseEntity<ApiResponse<T>> forbidden(String message, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(
                        ApiResponse.<T>builder()
                                .success(false)
                                .message(message)
                                .path(request.getRequestURI())
                                .build()
                );
    }

    /** 404 Not Found. */
    public static <T> ResponseEntity<ApiResponse<T>> notFound(String message, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(
                        ApiResponse.<T>builder()
                                .success(false)
                                .message(message)
                                .path(request.getRequestURI())
                                .build()
                );
    }

    /** 409 Conflict. */
    public static <T> ResponseEntity<ApiResponse<T>> conflict(String message, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        ApiResponse.<T>builder()
                                .success(false)
                                .message(message)
                                .path(request.getRequestURI())
                                .build()
                );
    }

    // -------------------------------------------------------------------------
    // 5xx Server Errors
    // -------------------------------------------------------------------------

    /** 500 Internal Server Error. */
    public static <T> ResponseEntity<ApiResponse<T>> internalError(String message, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ApiResponse.<T>builder()
                                .success(false)
                                .message(message)
                                .path(request.getRequestURI())
                                .build()
                );
    }

    // -------------------------------------------------------------------------
    // Generic — when you need full control over the status code
    // -------------------------------------------------------------------------

    /** Build a response with any HTTP status, data, and message. */
    public static <T> ResponseEntity<ApiResponse<T>> of(
            HttpStatus status, T data, String message, HttpServletRequest request) {
        return ResponseEntity
                .status(status)
                .body(
                        ApiResponse.<T>builder()
                                .success(status.is2xxSuccessful())
                                .message(message)
                                .data(data)
                                .path(request.getRequestURI())
                                .build()
                );
    }

    /** Build an error response with any HTTP status and error list. */
    public static <T> ResponseEntity<ApiResponse<T>> of(
            HttpStatus status, String message, List<ApiError> errors, HttpServletRequest request) {
        return ResponseEntity
                .status(status)
                .body(
                        ApiResponse.<T>builder()
                                .success(false)
                                .message(message)
                                .errors(errors)
                                .path(request.getRequestURI())
                                .build()
                );
    }
}
