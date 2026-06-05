package com.spring.JavaT.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates usernames: 3–50 characters, {@code [a-zA-Z0-9._-]}, must contain
 * at least one letter. Combine with {@link NoWhitespace} to reject padded values.
 */
@Documented
@Constraint(validatedBy = ValidUsernameValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUsername {

    String message() default ValidationMessages.USERNAME_INVALID;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
