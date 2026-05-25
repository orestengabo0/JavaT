package com.spring.JavaT.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a string value matches one of the constants in a given enum.
 *
 * <p>Useful when an enum value arrives as a plain string from a JSON request body
 * and you want a clear validation error instead of a generic deserialization failure.
 *
 * <p>{@code null} values are considered valid — combine with {@code @NotNull} if
 * the field is required.
 *
 * <p>Usage:
 * <pre>
 * {@literal @}NotNull(message = "Role is required")
 * {@literal @}ValidEnum(enumClass = Role.class, message = "Invalid role. Accepted values: ADMIN, USER")
 * private String role;
 * </pre>
 */
@Documented
@Constraint(validatedBy = ValidEnumValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEnum {

    /** The enum class whose constants are the accepted values. */
    Class<? extends Enum<?>> enumClass();

    /** Whether the comparison is case-insensitive. Defaults to {@code false}. */
    boolean ignoreCase() default false;

    String message() default ValidationMessages.ENUM_INVALID;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
