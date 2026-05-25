package com.spring.JavaT.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Standard API response wrapper used across all endpoints.
 *
 * <p>Every response from this API follows this structure:
 * <pre>
 * {
 *   "success": true,
 *   "message": "Operation successful",
 *   "data": { ... },
 *   "errors": null,
 *   "timestamp": "2024-01-01T00:00:00Z",
 *   "path": "/api/v1/users"
 * }
 * </pre>
 *
 * @param <T> the type of the response payload
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response envelope")
public class ApiResponse<T> {

    @Schema(description = "Indicates whether the request was successful", example = "true")
    private final boolean success;

    @Schema(description = "Human-readable message describing the result", example = "Operation successful")
    private final String message;

    @Schema(description = "Response payload; null on error responses")
    private final T data;

    @Schema(description = "List of validation or business errors; null on success responses")
    private final List<ApiError> errors;

    @Schema(description = "UTC timestamp of when the response was generated", example = "2024-01-01T00:00:00Z")
    private final Instant timestamp;

    @Schema(description = "The request path that produced this response", example = "/api/v1/users")
    private final String path;

    private ApiResponse(Builder<T> builder) {
        this.success   = builder.success;
        this.message   = builder.message;
        this.data      = builder.data;
        this.errors    = builder.errors;
        this.timestamp = Instant.now();
        this.path      = builder.path;
    }

    // -------------------------------------------------------------------------
    // Static factory shortcuts — use ResponseBuilder for the full fluent API
    // -------------------------------------------------------------------------

    /** Quick success response with data and a default message. */
    public static <T> ApiResponse<T> ok(T data) {
        return new Builder<T>().success(true).message("Operation successful").data(data).build();
    }

    /** Quick success response with data and a custom message. */
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new Builder<T>().success(true).message(message).data(data).build();
    }

    /** Quick success response with no payload (e.g. DELETE). */
    public static <T> ApiResponse<T> ok(String message) {
        return new Builder<T>().success(true).message(message).build();
    }

    /** Quick error response with a message and error list. */
    public static <T> ApiResponse<T> error(String message, List<ApiError> errors) {
        return new Builder<T>().success(false).message(message).errors(errors).build();
    }

    /** Quick error response with a single message and no error list. */
    public static <T> ApiResponse<T> error(String message) {
        return new Builder<T>().success(false).message(message).build();
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static final class Builder<T> {
        private boolean success;
        private String message;
        private T data;
        private List<ApiError> errors;
        private String path;

        public Builder<T> success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder<T> message(String message) {
            this.message = message;
            return this;
        }

        public Builder<T> data(T data) {
            this.data = data;
            return this;
        }

        public Builder<T> errors(List<ApiError> errors) {
            this.errors = errors;
            return this;
        }

        public Builder<T> path(String path) {
            this.path = path;
            return this;
        }

        public ApiResponse<T> build() {
            return new ApiResponse<>(this);
        }
    }
}
