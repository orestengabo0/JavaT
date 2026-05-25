package com.spring.JavaT.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a string has no leading or trailing whitespace.
 *
 * <p>Useful for fields like usernames and codes where accidental spaces
 * would cause hard-to-debug lookup failures.
 *
 * <p>{@code null} and empty strings are considered valid — combine with
 * {@code @NotBlank} if the field is required.
 *
 * <p>Usage:
 * <pre>
 * {@literal @}NotBlank(message = ValidationMessages.USERNAME_REQUIRED)
 * {@literal @}NoWhitespace
 * private String username;
 * </pre>
 */
@Documented
@Constraint(validatedBy = NoWhitespaceValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoWhitespace {

    String message() default ValidationMessages.NO_WHITESPACE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
