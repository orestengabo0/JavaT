package com.spring.JavaT.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * Represents a single error entry inside an {@link ApiResponse}.
 *
 * <p>Used for both validation errors (field-level) and business/application errors.
 *
 * <p>Example (validation):
 * <pre>
 * {
 *   "field": "email",
 *   "message": "must be a valid email address",
 *   "rejectedValue": "not-an-email",
 *   "code": "Email"
 * }
 * </pre>
 *
 * <p>Example (business error):
 * <pre>
 * {
 *   "message": "User with this email already exists",
 *   "code": "USER_ALREADY_EXISTS"
 * }
 * </pre>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Represents a single error detail within an API error response")
public class ApiError {

    @Schema(description = "The field that caused the error; null for non-field errors", example = "email")
    private final String field;

    @Schema(description = "Human-readable description of the error", example = "must be a valid email address")
    private final String message;

    @Schema(description = "The value that was rejected; null when not applicable", example = "not-an-email")
    private final Object rejectedValue;

    @Schema(description = "Machine-readable error code", example = "USER_ALREADY_EXISTS")
    private final String code;

    // -------------------------------------------------------------------------
    // Convenience factories
    // -------------------------------------------------------------------------

    /** Creates a field-level validation error. */
    public static ApiError ofField(String field, String message, Object rejectedValue, String code) {
        return ApiError.builder()
                .field(field)
                .message(message)
                .rejectedValue(rejectedValue)
                .code(code)
                .build();
    }

    /** Creates a field-level validation error without a rejected value. */
    public static ApiError ofField(String field, String message) {
        return ApiError.builder()
                .field(field)
                .message(message)
                .build();
    }

    /** Creates a global (non-field) error with a code. */
    public static ApiError ofGlobal(String message, String code) {
        return ApiError.builder()
                .message(message)
                .code(code)
                .build();
    }

    /** Creates a global (non-field) error without a code. */
    public static ApiError ofGlobal(String message) {
        return ApiError.builder()
                .message(message)
                .build();
    }
}
